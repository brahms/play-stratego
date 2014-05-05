package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import play.api.mvc.{AnyContent, Request, Action, Results}
import javax.inject.{Singleton, Named, Inject}
import brahms.database.GameRepository
import org.bson.types.ObjectId

@Named
@Singleton
class DebugController extends AbstractController{

  @Inject
  var gameRepo: GameRepository = _

  def getGame(id: String) = Action.async {
    implicit request =>
      async {
        gameRepo.findOne(new ObjectId(id)) match {
          case Some(game) =>
            Results.Ok(game.stateToString)
          case _ =>
            Results.NotFound
        }
      }
  }

  def getGames = Action.async { implicit request =>
    async {
      Results.Ok(gameRepo.findAll().map(_.stateToString).reduce(_ + "\n" + _))
    }
  }
}
