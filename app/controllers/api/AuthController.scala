package controllers.api

import javax.inject.{Inject, Singleton, Named}
import play.api.mvc.{Request, Action}
import brahms.util.AbstractController
import brahms.response.JsonResponse
import brahms.requests.AuthenticatedRequest
import play.api.libs.json.JsValue
import brahms.model.User
import org.springframework.validation.{ValidationUtils, Errors, Validator}
import brahms.database.UserRepository
import scala.beans.BeanProperty

@Named
@Singleton
class AuthController extends AbstractController {

  @Inject
  var userRepo: UserRepository = _

  def login: Action[JsValue]= Action.async(parse.json) {
    implicit r =>
      val username = (r.body \ "username").as[String]
      val password = (r.body \ "password").as[String]

      if (User.validateUsername(username) &&
          User.validatePassword(password)){
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
              JsonResponse.priv(user).withSession(sessionService.SESSION_TOKEN -> newSession.token)
            case _ =>
              JsonResponse.bad("Invalid login")
          }
        }
      }
      else {
        skipAsync(JsonResponse.bad("Invalid username/password passed in"))
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

  def signup = Action.async(parse.json) {
    implicit request: Request[JsValue] =>
      val username = (request.body \ "username").as[String]
      val password = (request.body \ "password").as[String]

      logger.debug("Attempting to create user: " + username)
      async {
        sessionService.getSession(request) match {
          case Right(session) =>
            JsonResponse.bad("Already logged in")
          case Left(_) =>
            validate(LoginRequest(username, password), LoginRequestValidator) match {
              case Right(loginRequest) =>

                var user = new User
                user.setUsername(username)
                user.setPassword(User.encryptPassword(loginRequest.password))
                user.setAdmin(false)

                logger.info("Request validated, creating user: " + user)
                user = userRepo.save(user)

                val session = sessionService.startSession(user, request)
                logger.debug("Creating new session: " + session.token)

                JsonResponse.ok(user).withSession(sessionService.SESSION_TOKEN -> session.token)

              case Left(validationError) =>
                validationError
            }
        }

      }
  }


  private case class LoginRequest(@BeanProperty username: String, @BeanProperty password: String)

  private object LoginRequestValidator extends Validator {
    override def supports(clazz: Class[_]): Boolean = {
      classOf[LoginRequest].equals(clazz)
    }

    override def validate(target: scala.Any, errors: Errors): Unit = {
      val req = target.asInstanceOf[LoginRequest]
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", req.username)
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", req.password)
      if (errors.hasErrors == false) {
        if (!User.validateUsername(req.username)) {
          errors.rejectValue("username", "invalid", s"Must match the pattern: ${User.USERNAME_PATTERN}")
        }
      }
      if (errors.hasErrors == false) {
        userRepo.findByUsername(req.username).map {
          _ =>
            errors.rejectValue("username", "unique", "Username is already taken")
        }
      }

      if (errors.hasErrors == false) {
        if (!User.validatePassword(req.password)) {
          errors.rejectValue("password", "invalid", s"Must be at least ${User.PASSWORD_MIN} characters")
        }
      }
    }
  }

}
