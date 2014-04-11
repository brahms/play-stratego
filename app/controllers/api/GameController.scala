package controllers.api

import brahms.util.AbstractController
import brahms.response.JsonResponse

class GameController extends AbstractController {
  def getGames = Authenticated {
    implicit request =>
      JsonResponse.ok
  }

  def getGameState(id: String) = Authenticated {
    implicit request =>
      JsonResponse.ok
  }

  def invokeAction(id: String) = Authenticated {
    implicit request =>
      JsonResponse.ok
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
