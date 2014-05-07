import brahms.database.{GameRepository, UserRepository}
import brahms.filters.{HerokuRequireSSLFilter, StrictTransportSecurityFilter, LoggingFilter}
import brahms.model.User
import brahms.util.WithLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.csrf.CSRFFilter

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
    repo.findByUsername("cbrahms") match {
      case Some(user) =>
      case _ =>
        val user = new User
        user.setAdmin(true)
        user.setPassword(User.encryptPassword("OneOne11"))
        user.setUsername("cbrahms")
        repo.save(user)
    }
  }

  override def onStop(app: Application): Unit = {
    context.refresh()
    context.stop()
  }
}

