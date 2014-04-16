package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse
import brahms.model.stratego.StrategoAction
import javax.inject.Inject
import brahms.database.GameRepository
import scala.beans.BeanProperty

class GameController extends AbstractController {

  @Inject
  @BeanProperty
  var gameRepo: GameRepository = _

  def getGames = Authenticated.async {
    implicit request =>
      async {

        JsonResponse.ok
      }
  }

  def getGameState(id: String) = Authenticated.async {
    implicit request =>
      async {

        JsonResponse.ok
      }
  }

  def invokeAction(id: String) = Authenticated.text {
    implicit request =>
      val action = serializer.readValue(request.body, classOf[StrategoAction])
      async {
        JsonResponse.ok

      }
  }


  def join(id: String) = Authenticated {
    implicit request =>
      JsonResponse.ok
  }

  def createGame = Authenticated {
    implicit request =>
      JsonResponse.ok
  }
}
