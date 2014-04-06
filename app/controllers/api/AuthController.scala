package controllers.api

import javax.inject.{Singleton, Named}
import play.api.mvc.Action
import play.api.mvc.BodyParsers._
import brahms.util.AbstractController
import brahms.response.JsonResponse
import brahms.requests.AuthenticatedRequest
import play.api.libs.json.JsValue


@Named
@Singleton
class AuthController extends AbstractController  {

  def login = Action.async(parse.json) {
      implicit r =>
        val username = (r.body \ "username").as[String]
        val password = (r.body \ "password").as[String]
        async {
          authService.authenticate(username, password) match {
            case Some(user) =>
              sessionService.getSession(r) match {
                case Right(session: AuthenticatedRequest[JsValue]) =>
                  logger.debug("Abandoning old session: " + session.token)
                  sessionService.abandonSession(r)
                case _ =>
              }
              val newSession = sessionService.startSession(user, r)
              logger.debug("Starting new session: " + newSession.token)
              JsonResponse.ok(user).withSession(sessionService.serverSideSessionTokenKeyName -> newSession.token)
            case _ =>
              JsonResponse.bad("Invalid login")
          }
        }
    }
  def logout = Action {
    implicit r =>
      sessionService.getSession(r) match {
        case Right(session) =>
          logger.debug("Abandoning old session: " + session.token)
          sessionService.abandonSession(session)
        case _ =>
      }
      JsonResponse.ok("Success").withNewSession
  }
}
