package brahms.actors

import brahms.model.{GameState, Game, GameAction, User}
import akka.actor.Actor
import javax.inject.{Inject, Named}
import org.springframework.context.annotation.Scope
import brahms.util.{Async, WithLogging}
import brahms.database.{GameRepository, UserRepository}
import brahms.model.stratego.{StrategoAction, StrategoGame}
import org.bson.types.ObjectId
import brahms.serializer.Serializer

case class CreateGame(user: User, gameType: String)
case class Failed(reason: String)
case class CreateGameSucceeded(game: Game)
case class JoinGame(user: User, gameId: String)
case class JoinGameSucceeded(game: Game)
case object SaveState

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
  var runningGames : Map[String, Game] = Map()

  def logReceiveTime(block: Receive) : Receive = {
    case some =>
      val start = System.currentTimeMillis()
      logger.debug("Starting receive, current games list is {} long", runningGames.size)
      block(some)
      logger.debug("Finished receive, current games list is {} long", runningGames.size)
      logger.info("Finished receive in {} ms", (System.currentTimeMillis() - start))
  }

  override def preStart(): Unit = {
    logger.info("Loading running games cache")
    runningGames = gameRepo.findRunning.foldLeft(Map[String, Game]())((map, game) => map + (game.id.toString -> game))
    runningGames.values.foreach({f=>assert(f.getState == GameState.RUNNING)})
    logger.info("Loaded {} games into cache", runningGames.size)
  }

  override def receive: Receive = logReceiveTime {
    case req: CreateGame if req.gameType.equals("stratego") =>
      logger.info("Handling a create game request: " + req)
      val user = userRepo.findByUsername(req.user.getUsername).get
      if(user.getCurrentGameId.isEmpty) {
        logger.debug("Creating stratego game for user: " + user)
        val game = new StrategoGame
        game.setRedPlayer(user)
        gameRepo.save(game)
        user.setCurrentGameId(Some(game.getId))
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

      if (runningGames.get(req.gameId).isDefined) {
        logger.warn("Game is already running cannot join: " + req.gameId)
        sender ! Failed("Game is not joinable: " + req.gameId)
      }
      else {
        val user = userRepo.findOne(req.user.getId).get
        if(user.getCurrentGameId.isEmpty) {
          gameRepo.findOnePending(new ObjectId(req.gameId)) match {
            case Some(game: StrategoGame) if game.state == GameState.PENDING =>
              logger.debug(s"$user joining game ${game}")
              game.setBluePlayer(user)
              game.init
              gameRepo.save(game)
              runningGames += (game.getId.toString -> game)
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
    case SaveState =>
      logger.debug("Saving state")
      gameRepo.save(runningGames.values)
      logger.debug("Saved")
    case msg =>
      logger.warn("Failed: Unknown msg received: {}", msg)
      sender ! Failed("Unknown")
  }
}
