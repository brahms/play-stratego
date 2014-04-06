package controllers

import play.api._
import play.api.mvc._
import brahms.util.WithLogging
import brahms.model.User

object Application extends Controller with WithLogging {

  def index = Action {
    logger.debug("hi")
    Ok(views.html.index("Your new application is ready."))
  }

}