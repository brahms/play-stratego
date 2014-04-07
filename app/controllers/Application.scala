package controllers

import play.api._
import play.api.mvc._
import brahms.util.{AbstractController, WithLogging}
import brahms.model.User
import javax.inject.{Inject, Singleton, Named}
import brahms.database.UserRepository
import brahms.serializer.JsonViews

@Named
@Singleton
class Application extends AbstractController with WithLogging {

  def index = Action.async {
    implicit request =>
      async {
        sessionService.getSession(request) match {
          case Right(session) =>
            Ok(views.html.index(userJson=serializer.writerWithView(PUBLIC).writeValueAsString(session.user)))
          case _ =>
            Ok(views.html.index(userJson=""))
        }
      }
  }

  def index2(rest: String) = index

}