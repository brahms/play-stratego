package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import brahms.model.stratego.{StrategoTypes, StrategoGame, StrategoAction}
import javax.inject.{Named, Singleton, Inject}
import brahms.database.{UserRepository, GameRepository}
import scala.beans.BeanProperty
import org.bson.types.ObjectId
import brahms.model.{User, Game, GameState}
import play.api.mvc.Action
import brahms.model.stratego.StrategoActions.PlacePieceAction
import brahms.model.stratego.StrategoTypes.{BluePiece, RedPiece}
import org.springframework.beans.factory.InitializingBean
import akka.actor.ActorRef
import brahms.actors._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._


@Singleton
@Named
class GameController extends AbstractController with InitializingBean {

  @Inject
  @BeanProperty
  var gameRepo: GameRepository = _

  @Inject
  var userRepo: UserRepository = _

  @Inject
  var actorSystem: ActorSystemBean = _

  var gameManager: ActorRef = _


  implicit val timeout = Timeout((5 seconds))

  override def afterPropertiesSet(): Unit = {
    logger.debug("afterPropertiesSet called")
    gameManager = actorSystem.gameManager
  }

  /**
   * Returns all games, by default it returns games in the pending state, the intention is to be able
   * to build a game list for a player to find a game to join
   * @return
   */
  def getGames = Authenticated.async {
    implicit request =>
      async {
        val res = gameRepo.findPending.map {
          g =>
            Map(
              "_id" -> g.getId.toString,
              "creator" -> g.getCreator.toSimpleUser,
              "type" -> g.getType,
              "state" -> g.getState.toString
            )
        }
        JsonResponse.ok(res)
      }
  }

  /**
   * Get's the current or historical game state of a game, if the game is currently in the RUNNING state
   * this call will not return any state unless the user is part of the given game. If the user is currently in the
   * game it will mask the return value so that the user cannot cheat. If the game is finished all state will be returned
   * without any masking.
   * @param id The game id
   * @return
   */
  def getGameState(id: String) = Authenticated.async {
    implicit request =>
      async {
        gameRepo.findOne(new ObjectId(id)) match {
          case Some(game) if game.state == GameState.FINISHED || game.state == GameState.PENDING || request.user.isAdmin =>
            JsonResponse.ok(game)
          case Some(game) if id.equals(request.user.currentGameId.orNull) =>
            JsonResponse.ok(game.mask(request.user))
          case _ =>
            JsonResponse.notFound
        }
      }
  }

  /**
   * Retrieves the the action list for a game where each action is greater than the given id
   * The intended usage of this call is to poll for updates to a game
   * @param id The last action id the caller has seen
   * @return
   */
  def getGameActions(id: String) = Authenticated.async {
    implicit request =>
      val lastActionId = request.getQueryString("lastActionId").map(_.toInt).getOrElse(0)
      async {
        gameRepo.findOne(new ObjectId(id)) match {
          case Some(game) if game.state == GameState.FINISHED || request.user.isAdmin =>
            JsonResponse.ok(game.getActionList.filter(_.actionId > lastActionId))
          case Some(game) if id.equals(request.user.currentGameId.orNull) =>
            JsonResponse.ok(game.mask(request.user).getActionList.filter(_.actionId > lastActionId))
          case _ =>
            JsonResponse.notFound
        }
      }

  }

  /**
   * Attempts to invoke the given action passed through the request's body as json for the user's
   * current game which they should also be passing up in the request's url
   * @param id The game id to invoke the action for
   * @return
   */
  def invokeAction(id: String) = Authenticated.text {
    implicit request =>
      val action = serializer.readValue(request.body, classOf[StrategoAction])
      request.user.currentGameId match {
        case Some(gameId) if id.equals(gameId.toString) =>
          ask(gameManager, InvokeActionRequest(request.user, request.body)).map {
            case InvokeActionSucceeded =>
              JsonResponse.ok
            case Failed(reason) =>
              JsonResponse.bad(reason)
          }
        case _ =>
          logger.warn(s"${request.user.currentGameId} != $id")
          notAsync(JsonResponse.bad("Not currently in that game"))
      }

  }


  /**
   * This request attempts to allow the authenticated user to join the requested game
   * @param id The game id to join
   * @return
   */
  def join(id: String) = Authenticated.async {
    implicit request =>
      request.user.currentGameId match {
        case Some(id) =>
          notAsync(JsonResponse.bad("Already in a game"))
        case _ =>
          ask(gameManager, JoinGame(request.user, id)).map {
            case JoinGameSucceeded(game) =>
              JsonResponse.ok(game.mask(request.user))
            case Failed(reason) =>
              JsonResponse.bad(reason)
          }

      }

  }

  def createGame = Authenticated.async(parse.json) {
    implicit request =>
      request.user.getCurrentGameId match {
        case Some(gameId) =>
          notAsync(JsonResponse.bad("Already have a current game id: {}", gameId.toString))
        case _ =>
          val js = request.body
          (js \ "type").as[String] match {
            case "stratego" =>
              ask(gameManager, CreateGame(request.user, "stratego")).map {
                case CreateGameSucceeded(game) =>
                  JsonResponse.ok(game)
                case Failed(reason) =>
                  JsonResponse.bad(reason)
              }
            case str =>
              notAsync(JsonResponse.bad("Unknown game type " + str))
          }

      }
  }

  def getTestGameState = Action {
    implicit request =>
      request.getQueryString("blue") match {
        case Some(_) =>
          JsonResponse.ok(generateGameStateAsBlue)
        case _ =>
          JsonResponse.ok(generateGameStateAsRed)

      }
  }

  def getTestGameActions = Action {
    implicit request =>
      request.getQueryString("blue") match {
        case Some(_) =>
          JsonResponse.ok(generateGameStateAsBlue.getActionList)
        case _ =>
          JsonResponse.ok(generateGameStateAsRed.getActionList)

      }
  }

  val red = new User
  val blue = new User
  red.setUsername("red")
  red.setId(new ObjectId())
  blue.setUsername("blue")
  blue.setId(new ObjectId())

  def generateGameState: Game = {

    val game = new StrategoGame
    game.state = GameState.RUNNING
    game.setRedPlayer(red)
    game.setBluePlayer(blue)
    game.init


    var action: StrategoAction = PlacePieceAction(red, 1, 1, new RedPiece(StrategoTypes.GENERAL_9))
    action.invoke(game)
    action = PlacePieceAction(blue, 1, 10, new BluePiece(StrategoTypes.GENERAL_9))
    action.invoke(game)
    game
  }

  def generateGameStateAsRed: Game = {
    generateGameState.mask(red)
  }

  def generateGameStateAsBlue: Game = {
    generateGameState.mask(blue)
  }
}
