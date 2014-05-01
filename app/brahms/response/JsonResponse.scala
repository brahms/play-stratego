package brahms.response

import play.api.mvc.{SimpleResult, Request}
import play.api.mvc.Results._
import brahms.serializer.{JsonViews, Serializer}
import play.api.http._
import play.api.Logger

object JsonResponse {


  def priv[A, B](obj: A)(implicit request: Request[B]) : SimpleResult = {
    val json = Serializer.serializer.writerWithView(JsonViews.PRIVATE).writeValueAsString(obj)
    Logger.info(s"Returning: '$json'");
    Ok(json)
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
  }
  def ok[A, B](obj: A)(implicit request: Request[B]) : SimpleResult= {
    val json = Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(obj)
    Logger.info(s"Returning: '$json'");
    Ok(json)
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
  }
  def ok[B](implicit request: Request[B]) : SimpleResult= {
    ok("Success")
  }

  def bad[A,B](obj: A)(implicit request: Request[B]) : SimpleResult= {
    BadRequest(Serializer.serializer.writeValueAsString(obj))
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
  }
  def bad[A,B](implicit request: Request[B]): SimpleResult = {
    bad("Server Error")
  }
  def notFound[B](implicit request: Request[B]): SimpleResult = {
    NotFound("Not found")
  }
}
