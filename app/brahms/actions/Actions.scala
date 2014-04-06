package brahms.actions
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Actions {
  def onlyHttps[A](action: Action[A]) = Action.async(action.parser) { request =>
    request.headers.get("X-Forwarded-Proto").collect {
      case "https" => action(request)
    } getOrElse {
      Future.successful(Forbidden("Only HTTPS requests allowed"))
    }
  }

}
