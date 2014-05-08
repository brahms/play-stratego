package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import javax.inject.{Named, Singleton, Inject}
import brahms.database.{UserRepository, GameRepository}
import scala.beans.BeanProperty
import brahms.model.GameState
import brahms.model.stratego.StrategoTypes.RedPiece
import org.springframework.beans.factory.InitializingBean
import akka.actor.ActorRef
import brahms.actors._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import brahms.model.stratego.{StrategoGame, StrategoAction}
import org.joda.time.DateTime


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
        JsonResponse.ok(gameRepo.findPending.toArray)
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
        logger.debug("Attempting to find game id {}", id)
        gameRepo.findOne(id) match {
          case Some(game) if game.state == GameState.FINISHED || game.state == GameState.PENDING || request.user.isAdmin =>
            JsonResponse.ok(game)
          case Some(game) if id.equals(request.user.currentGameId.map(_.toString).orNull) =>
            JsonResponse.ok(game.mask(request.user))
          case _ =>
            JsonResponse.notFound
        }
      }
  }

  /**
   * Retrieves the the action list for a game where each action is greater than the given id
   * The intended usage of this call is to poll for updates to a game
   * @param id The last action id the caller has en
   * @return
   */
  def getGameActions(id: String) = Authenticated.async {
    implicit request =>
      val lastActionId = request.getQueryString("lastActionId").map(_.toInt).getOrElse(0)

      logger.debug("Got for last action id: {}", lastActionId)
      async {
        gameRepo.findOne(id) match {
          case Some(game: StrategoGame) if game.state == GameState.FINISHED || request.user.isAdmin =>
            JsonResponse.ok(game.getActionList.filter(_.actionId > lastActionId).toArray)
          case Some(game: StrategoGame) if id.equals(request.user.currentGameId.map(_.toString).orNull) =>
            JsonResponse.ok(game.mask(request.user).getActionList.filter(_.actionId > lastActionId).toArray)
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
          notAsync(JsonResponse.bad("Already have a current game id: " + gameId))
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

  def cancelCurrentGame = Authenticated {
    implicit request =>
      request.user.currentGameId match {
        case Some(id) =>
          gameManager ! CancelGame(request.user)
          JsonResponse.ok(request.user)
        case _ =>
          JsonResponse.ok("Not in a game")
      }
  }

  def replays = Authenticated.async {
    implicit request =>
      async {
        JsonResponse.ok(gameRepo.findFinished.map {
          game =>
            Map (
              "id" -> game.id,
              "players" -> game.getGameStatistics.players.map(_.username),
              "winners" -> game.getGameStatistics.winners.map(_.username),
              "losers" -> game.getGameStatistics.losers.map(_.username),
              "draws" -> game.getGameStatistics.draws.map(_.username),
              "date" -> new DateTime().toString()
            )
        })
      }
  }
}
