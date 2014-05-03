package brahms.actors

import brahms.model.{GameState, Game, User}
import akka.actor.Actor
import javax.inject.{Inject, Named}
import org.springframework.context.annotation.Scope
import brahms.util.{Async, WithLogging}
import brahms.database.{GameRepository, UserRepository}
import brahms.model.stratego.{StrategoAction, StrategoGame}
import brahms.serializer.Serializer

case class CreateGame(user: User, gameType: String)
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

  /**
   * In memory map of games in the running state, to speed up action invocation
   */
  private var pendingGames : Map[String, Game] = Map()
  private var runningGames : Map[String, Game] = Map()

  def logReceiveTime(block: Receive) : Receive = {
    case some =>
      val start = System.currentTimeMillis()
      logger.debug("Starting receive, current pending games list is {} long", pendingGames.size)
      logger.debug("Starting receive, current running games list is {} long", runningGames.size)
      block(some)
      logger.debug("Finished receive, current pending games list is {} long", pendingGames.size)
      logger.debug("Finished receive, current running games list is {} long", runningGames.size)
      logger.info("Finished receive in {} ms", (System.currentTimeMillis() - start))
  }

  override def preStart(): Unit = {
    logger.info("Loading pending games into cache")
    pendingGames = gameRepo.findPending.foldLeft(Map[String, Game]())((map, game) => map + (game.id.toString -> game))
    logger.info("Loaded {} pending games into cache", pendingGames.size)
    logger.info("Loading running games cache")
    runningGames = gameRepo.findRunning.foldLeft(Map[String, Game]())((map, game) => map + (game.id.toString -> game))
    runningGames.values.foreach({f=>assert(f.getState == GameState.RUNNING)})
    logger.info("Loaded {} running games into cache", runningGames.size)
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
        user.setCurrentGameId(Some(game.getId))
        logger.debug("Setting current user game to {}", Serializer.serializer.writeValueAsString(user))
        userRepo.save(user)
        pendingGames += game.getId.toString -> game
        sender ! CreateGameSucceeded(game)
        logger.debug("Sent back CreateGameSucceeded msg ")
      }
      else {
        logger.warn("Failed: user already in game: " + user.getCurrentGameId)
        sender ! Failed("user already in game: " + user.getCurrentGameId.get)
      }
    case req: JoinGame =>
      logger.info("Handling a joinGame request: " + req)

      if (runningGames.get(req.gameId).isDefined) {
        logger.warn("Game is already running cannot join: " + req.gameId)
        sender ! Failed("Game is not joinable: " + req.gameId)
      }
      else {
        val user = userRepo.findByUsername(req.user.username).get
        if(user.getCurrentGameId.isEmpty) {
          pendingGames.get(req.gameId) match {
            case Some(game: StrategoGame) if game.state == GameState.PENDING =>
              logger.debug(s"$user joining game ${game}")
              pendingGames -= game.getId.toString
              runningGames += (game.getId.toString -> game)
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
          runningGames.get(id.toString) match {
            case Some(game : StrategoGame) =>
              val action = Serializer.serializer.readValue(req.actionJson, classOf[StrategoAction])

              if (action.isLegal(game)) {
                logger.debug("Action is legal, invoking: {}", action)
                action.invoke(game)
                logger.debug("Invoked {}", action)
                logger.debug("Saving invoked game: {}", game.id)
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
      pendingGames.values.foreach { game =>
        game.timeouts.foreach { case (username: String, timeout: Long) =>
          if (timeout < timeoutCheck) {
            logger.warn(s"$username timed out in game $game")
            logger.warn("Game is pending, so canceling it")
            pendingGames -= game.id.toString
            game.state = GameState.CANCELED
            gameRepo.save(game)
            val user = userRepo.findByUsername(username).get
            user.setCurrentGameId(None)
            logger.debug("Setting user to have no game set: {}", user)
            userRepo.save(user)
          }
        }
      }
      runningGames.values.foreach {  game =>
          game.timeouts.foreach {  case (username: String, timeout: Long) =>
              if (timeout < timeoutCheck) {
                runningGames -= game.id.toString
                val user = userRepo.findByUsername(username).get
                logger.warn(s"$user timed out in game $game")
                game.handleTimeout(user)
                assert(game.isGameOver)
                gameRepo.save(game)
                val stats = game.gameStats
                userRepo.updateUserStats(stats)
              }
          }
      }
      logger.debug("Done checking for timeouts")
    case msg =>
      logger.warn("Failed: Unknown msg received: {}", msg)
      sender ! Failed("Unknown")
  }
}
