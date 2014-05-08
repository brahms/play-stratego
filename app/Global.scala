import brahms.database.{GameRepository, UserRepository}
import brahms.filters.{HerokuRequireSSLFilter, StrictTransportSecurityFilter, LoggingFilter}
import brahms.model.User
import brahms.util.WithLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.csrf.CSRFFilter
import play.api.Play
import play.api.Play.current
object Global extends WithFilters(HerokuRequireSSLFilter, CSRFFilter(), LoggingFilter, StrictTransportSecurityFilter) with WithLogging{
  val context = new AnnotationConfigApplicationContext()

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    logger.debug(s"creating bean of class $controllerClass")
    context.getBean(controllerClass)
  }

  override def onStart(app: Application): Unit = {
    logger.debug("onStart")
    context.scan("brahms")
    context.scan("controllers")
    context.refresh()
    context.start()

    logger.debug("Searching for initial cbrahms user")
    val repo = context.getBean(classOf[UserRepository])

    Play.configuration.getString("adminuser").foreach {
      name =>
        Play.configuration.getString("adminpass").foreach {
          pass =>
            repo.findByUsername(name) match {
              case Some(user) =>
              case _ =>
                val user = new User
                user.setAdmin(true)
                user.setPassword(User.encryptPassword(pass))
                user.setUsername(name)
                repo.save(user)
                logger.info("Creating admin user: " + user)
            }
        }
    }
  }

  override def onStop(app: Application): Unit = {
    context.refresh()
    context.stop()
  }
}

