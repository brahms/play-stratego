package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import brahms.model.stratego.{StrategoGame, StrategoAction}
import javax.inject.Inject
import brahms.database.{UserRepository, GameRepository}
import scala.beans.BeanProperty
import org.bson.types.ObjectId
import brahms.model.GameState

class GameController extends AbstractController {

  @Inject
  @BeanProperty
  var gameRepo: GameRepository = _

  @Inject
  var userRepo: UserRepository = _
  
  def getGames = Authenticated.async {
    implicit request =>
      async {
        val res = gameRepo.findPending.map {
          g =>
            Map (
              "_id" -> g.getId.toString,
              "creator" -> g.getCreator.toSimpleUser,
              "type" -> g.getType,
              "state" -> g.getState.toString
            )
        }
        JsonResponse.ok(res)
      }
  }

  def getGameState(id: String) = Authenticated.async {
    implicit request =>
      async {
          gameRepo.findOne(new ObjectId(id)) match {
            case Some(game) if game.state == GameState.FINISHED || request.user.isAdmin=>
              JsonResponse.ok(game)
            case Some(game) if id.equals(request.user.currentGameId.orNull) =>
              JsonResponse.ok(game.mask(request.user))
            case _ =>
              JsonResponse.notFound
        }
      }
  }

  def invokeAction(id: String) = Authenticated.text {
    implicit request =>
      val action = serializer.readValue(request.body, classOf[StrategoAction])
      async {
        request.user.currentGameId match {
          case Some(gameId) if id.equals(gameId) =>
            JsonResponse.bad("Not impelemented")
          case _ =>
            JsonResponse.bad("Not currently in that game")
        }
      }
  }


  def join(id: String) = Authenticated.async {
    implicit request =>
      async {
        request.user.currentGameId match {
          case Some(id) =>
            JsonResponse.bad("Already in a game")
          case _ =>
            val game = gameRepo.findOne(new ObjectId(id))
            game match {
              case game: StrategoGame if game.state == GameState.PENDING =>
                game.setBluePlayer(request.user)
                request.user.setCurrentGameId(Option(game.id))
                game.init
                gameRepo.save(game)
                userRepo.save(request.user)
                JsonResponse.ok(game.mask(request.user))
            }
        }
      }
  }

  def createGame = Authenticated.async(parse.json) {
    implicit request =>
      async {
        request.user.getCurrentGameId match {
          case Some(gameId) =>
            JsonResponse.bad("Already have a current game id: {}", gameId.toString)
          case _ =>
            val js = request.body
            (js \ "type").as[String] match {
              case "stratego" =>
                val game = new StrategoGame
                game.setRedPlayer(request.user)
                gameRepo.save(game)
                request.user.setCurrentGameId(Option(game.getId))
                userRepo.save(request.user)
                JsonResponse.ok(game.getId)
              case str =>
                JsonResponse.bad("Unknown game type " + str)
            }
        }
      }
  }
}
