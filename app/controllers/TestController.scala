package controllers

import play.api.mvc.{Controller, Action}

object TestController extends Controller {

  def test = Action {
    Ok(views.html.test())
  }
  def test2 = Action {
    Ok(views.html.test2())
  }
}
