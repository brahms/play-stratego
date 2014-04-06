package brahms.service

import javax.inject.Inject
import brahms.database.UserRepository
import brahms.model.User
import play.api.Logger
import org.springframework.stereotype.Service
import brahms.util.WithLogging

@Service
class AuthenticationService extends WithLogging{
  @Inject
  var userRepo: UserRepository = _
  def authenticate(username: String, password: String) : Option[User] = {
    userRepo.findByUsername(username) match {
      case Some(user) if(user.validatePassword(password)) =>
        logger.info(s"Authenticated $user")
        Some(user)
      case _ =>
        logger.info(s"Invalid authentication: $username")
        None
    }
  }
}
