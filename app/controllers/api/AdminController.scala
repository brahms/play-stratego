package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import play.api.mvc._
import javax.inject.{Singleton, Named, Inject}
import brahms.database.{UserRepository, GameRepository}
import org.bson.types.ObjectId
import scala.Some
import brahms.model.Game

@Named
@Singleton
/**
 * Some utility routes only available to admins
 */
class AdminController extends AbstractController{

  @Inject
  var gameRepo: GameRepository = _

  @Inject
  var userRepo: UserRepository = _

  def getGame(id: String) = Authenticated.admin {
    implicit request =>
      async {
        gameRepo.findOne(id) match {
          case Some(game) =>
            Results.Ok(game.stateToString)
          case _ =>
            Results.NotFound
        }
      }
  }

  def getGames = Authenticated.admin { implicit request =>
    async {
      Results.Ok(gameRepo.findAll().foldLeft[String]("")((str, game) => str + game.stateToString + "\n"))
    }
  }

  def reset = Authenticated.admin {implicit request =>
    userRepo.deleteAll()
    gameRepo.deleteAll()
    System.exit(-1)
    notAsync(JsonResponse.ok(""))
  }

  def getUsers = Authenticated.admin {implicit request =>
    async {
      JsonResponse.ok(userRepo.findAll())
    }
  }

}
