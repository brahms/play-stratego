package controllers.api

import brahms.util.AbstractController
import javax.inject.{Singleton, Named, Inject}
import brahms.response.JsonResponse
import brahms.requests.AuthenticatedRequest
import play.api.mvc.{AnyContent, Action}
import play.api.mvc.BodyParsers._
import brahms.database.UserRepository
import brahms.model.User

@Named
@Singleton
class UserController extends AbstractController {

  @Inject
  var userRepo: UserRepository = _

  def users = Authenticated.text {
    implicit r: AuthenticatedRequest[String] =>
      async {
        JsonResponse.ok("hi")
      }
  }

  def isUsernameUnique = Action.async(parse.json) {
    implicit r =>
      val username = (r.body \ "username").as[String]
      async {
        userRepo.findByUsername(username) match {
          case Some(_)  =>
            JsonResponse.ok(Map("status" -> false))
          case _ =>
            JsonResponse.ok(Map("status" -> true))
        }
      }
  }

  def self = Authenticated {
    implicit request: AuthenticatedRequest[AnyContent] =>
      logger.debug("self: {}", request.user.toJson)
      JsonResponse.priv(request.user)
  }
}
