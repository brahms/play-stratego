import brahms.database.{GameRepository, UserRepository}
import brahms.filters.LoggingFilter
import brahms.model.User
import brahms.util.WithLogging
import org.bson.types.ObjectId
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import play.api.{Application, GlobalSettings}
import play.api.mvc.WithFilters

object Global extends WithFilters(LoggingFilter) with WithLogging{
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

    val repo = context.getBean(classOf[UserRepository])
    logger.debug("Searching for initial cbrahms user")

    repo.deleteAll()
    val user = new User
    user.setAdmin(true)
    user.setPassword(User.encryptPassword("OneOne11"))
    user.setUsername("cbrahms")
    repo.save(user)

    val gameRepo = context.getBean(classOf[GameRepository])
    logger.debug("Deleting all games")
    gameRepo.deleteAll()

  }

  override def onStop(app: Application): Unit = {
    context.refresh()
    context.stop()
  }
}

