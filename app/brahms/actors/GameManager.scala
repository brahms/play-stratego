package brahms.actors

import brahms.model.{GameState, Game, User}
import akka.actor.Actor
import javax.inject.{Inject, Named}
import org.springframework.context.annotation.Scope
import brahms.util.{Async, WithLogging}
import brahms.database.{GameRepository, UserRepository}
import brahms.model.stratego.{StrategoAction, StrategoGame}
import brahms.serializer.Serializer
import org.joda.time.DateTime

case class CreateGame(user: User, gameType: String)
case class CancelGame(user: User)
case class Failed(reason: String)
case class CreateGameSucceeded(game: Game)
case class JoinGame(user: User, gameId: String)
case class JoinGameSucceeded(game: Game)
case object CheckTimeout
case class InvokeActionRequest(user: User, actionJson: String)
case object InvokeActionSucceeded




/**
 * An actor that manages the life cycle of games so we don't have to deal with threading issues
 */
@Named
@Scope("prototype")
class GameManager @Inject() (userRepo: UserRepository, gameRepo: GameRepository) extends Actor with WithLogging with Async {

  def logReceiveTime(block: Receive) : Receive = {
    case some =>
      val start = System.currentTimeMillis()
      block(some)
      logger.info("Finished receive in {} ms", (System.currentTimeMillis() - start))
  }


  override def receive: Receive = logReceiveTime {
    case req: CreateGame if req.gameType.equals("stratego") =>
      logger.info("Handling a create game request: " + req)
      val user = userRepo.findByUsername(req.user.getUsername).get
      if(user.getCurrentGameId.isEmpty) {
        logger.debug("Creating stratego game for user: " + user)
        val game = new StrategoGame
        game.setRedPlayer(user.toSimpleUser)
        game.setCreator(user.toSimpleUser)
        game.players = Seq(user.toSimpleUser)
        game.timeouts = Map(user.username -> (System.currentTimeMillis() + Game.TIMEOUT))
        gameRepo.save(game)
        user.setCurrentGameId(Some(game.getId.toString))
        logger.debug("Setting current user game to {}", Serializer.serializer.writeValueAsString(user))
        userRepo.save(user)
        sender ! CreateGameSucceeded(game)
        logger.debug("Sent back CreateGameSucceeded msg ")
      }
      else {
        logger.warn("Failed: user already in game: " + user.getCurrentGameId)
        sender ! Failed("user already in game: " + user.getCurrentGameId.get)
      }
    case req: JoinGame =>
      logger.info("Handling a joinGame request: " + req)

      if (gameRepo.findOneRunning(req.gameId).isDefined) {
        logger.warn("Game is already running cannot join: " + req.gameId)
        sender ! Failed("Game is not joinable: " + req.gameId)
      }
      else {
        val user = userRepo.findByUsername(req.user.username).get
        if(user.getCurrentGameId.isEmpty) {
          gameRepo.findOnePending(req.gameId) match {
            case Some(game: StrategoGame) if game.state == GameState.PENDING =>
              logger.debug(s"$user joining game ${game}")
              game.setBluePlayer(user.toSimpleUser)
              game.init
              game.players = game.players :+ user
              gameRepo.save(game)
              user.setCurrentGameId(Some(game.getId))
              userRepo.save(user)
              sender ! JoinGameSucceeded(game)
              logger.debug("Sent back JoinGameSucceeded")
            case _ =>
              logger.warn("Failed, no game id: " + req.gameId)
              sender ! Failed("No game found for id: " + req.gameId)
          }
        }
        else {
          logger.warn("Failed: Already in game: " + user)
          sender ! Failed("Already in game: " + user)
        }
      }
    case req: InvokeActionRequest =>
      logger.info("Handling a invokeAction request: " + req)
      req.user.getCurrentGameId match {
        case Some(id) =>
          gameRepo.findOneRunning(id.toString) match {
            case Some(game : StrategoGame) =>
              val action = Serializer.serializer.readValue(req.actionJson, classOf[StrategoAction])
              action.setUser(req.user.toSimpleUser)
              if (action.isLegal(game)) {
                logger.debug("Action is legal, invoking: {}", action)
                action.invoke(game)
                logger.debug("Invoked {}", action)

                if (game.isGameOver) {
                  logger.info("Game is over")
                  val gameStats = game.getGameStatistics
                  userRepo.updateUserStats(gameStats)
                }

                logger.debug("Saving invoked game: {}", game.id)
                game.finishedDate = new DateTime()
                gameRepo.save(game)


                sender ! InvokeActionSucceeded
              }
              else {
                logger.warn(s"Failed: Cannot invoke stratego action (not legal): $action on game: \n${game.stateToString}")
                sender ! Failed("Illegal action")
              }
            case _ =>
              logger.error(s"Failed: User ${req.user} set with a game id of $id but not found in ingame memory map")
              sender ! Failed ("Unknown")
          }

        case _ =>
          logger.warn("Failed: User is not currently in a game")
          sender ! Failed("User is not currently in a game")
      }

    case CheckTimeout =>

      logger.debug("Checking for timeouts")

      val timeoutCheck = System.currentTimeMillis()
      gameRepo.findPending.foreach { game =>
        game.timeouts.foreach { case (username: String, timeout: Long) =>
          if (timeout < timeoutCheck) {
            logger.warn(s"$username timed out in game $game")
            logger.warn("Game is pending, so canceling it")
            game.state = GameState.CANCELED
            gameRepo.save(game)
            val user = userRepo.findByUsername(username).get
            user.setCurrentGameId(None)
            logger.debug("Setting user to have no game set: {}", user)
            userRepo.save(user)
          }
        }
      }
      gameRepo.findRunning.foreach {  game =>
          game.timeouts.foreach {  case (username: String, timeout: Long) =>
              if (timeout < timeoutCheck) {
                val user = userRepo.findByUsername(username).get
                logger.warn(s"$user timed out in game $game")
                game.handleTimeout(user)
                assert(game.isGameOver)
                game.finishedDate = new DateTime()
                gameRepo.save(game)
                val stats = game.getGameStatistics
                userRepo.updateUserStats(stats)
              }
          }
      }
      logger.debug("Done checking for timeouts")
    case CancelGame(user) =>

      user.currentGameId match {
        case Some(id) =>
          val gameId = id.toString
          if (gameRepo.findOnePending(gameId).isDefined) {
            val game = gameRepo.findOne(gameId).get
            logger.debug("Canceling game: {}", game)
            game.setState(GameState.CANCELED)
            gameRepo.save(game)
            user.setCurrentGameId(None)
            userRepo.save(user)
          }
        case _ =>
          logger.warn("Ignoring cancelGame Request from user who has no game id: {}", user)
      }
    case msg =>
      logger.warn("Failed: Unknown msg received: {}", msg)
      sender ! Failed("Unknown")
  }
}

