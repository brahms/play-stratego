package controllers.api

import brahms.util.AbstractController
import javax.inject.{Singleton, Named, Inject}
import brahms.response.JsonResponse
import brahms.requests.AuthenticatedRequest
import play.api.mvc.Action

@Named
@Singleton
class UserController extends AbstractController {

  def users = Authenticated.text {
    implicit r: AuthenticatedRequest[String] =>
      async {
        JsonResponse.ok("hi")
      }
  }

  def noauth = Action {
    implicit r =>
      JsonResponse.ok("hi")
  }
}
