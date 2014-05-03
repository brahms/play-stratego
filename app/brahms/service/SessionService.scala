package brahms.service

import javax.inject.{Inject, Named}
import org.springframework.stereotype.Service
import brahms.model.User
import play.api.mvc.Request
import brahms.cache.{ExpiredSessionError, NoSessionError, ServerSideSessions, ServerSideSessionState}
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import scala.concurrent.duration._
import brahms.requests.AuthenticatedRequest
import brahms.database.UserRepository


@Service
class SessionService {
  val SESSION_TOKEN = "token"
  val SESSION_EXPIRY = 24 hours

  @Inject
  var userRepo: UserRepository = _

  import play.api.Play.current // impliciit app

  def startSession(user: User): String = {
    val state = new ServerSideSessionState()
    state.user = Some(user)
    val token = ServerSideSessions.create(state, SESSION_EXPIRY)
    token
  }

  /**
   * This is invoked to find an existing server-side session for the given request. If the
   * system finds a session it retrieves the user, and if the user data is present then
   * an AuthenticatedRequest is created and then provided downstream. This retrieval is
   * what allows page loads to protected areas to succeed without requiring the user to
   * authenticate each time.
   */
  def getSession[A](request: Request[A]): Either[Exception, AuthenticatedRequest[A]] = {
    //println("XXX getSession: token->" + request.session.get(SESSION_TOKEN))
    for {
      token <- request.session.get(SESSION_TOKEN).toRight(new NoSessionError).right
      session <- ServerSideSessions.get(token).toRight(new ExpiredSessionError).right
      user <- session.user.toRight(new ExpiredSessionError).right
    } yield AuthenticatedRequest(userRepo.get(user), token, request)

  }

  /**
   * This is invoked to delete any existing server-side session, e.g. when logging out.
   */
  def abandonSession[A](request: Request[A]): Request[A] = {
   // println("XXX abandonSession: "+request.session.get(SESSION_TOKEN))
    request.session.get(SESSION_TOKEN).map(ServerSideSessions.delete)
    request
  }
}


