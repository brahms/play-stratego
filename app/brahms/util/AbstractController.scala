package brahms.util

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.Executors
import brahms.cache.{ExpiredSessionError, NoSessionError}
import javax.inject.Inject
import brahms.service.{SessionService, AuthenticationService}
import brahms.requests.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc.BodyParsers._
import play.api.libs.json.JsValue
import org.springframework.validation.{DataBinder, Validator}
import brahms.response.JsonResponse
import brahms.requests.AuthenticatedRequest
import play.api.mvc.SimpleResult
import brahms.serializer.{JsonViews, Serializer}

import scala.concurrent.Future
object AbstractController {
  val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
}

class AbstractController extends Controller with WithLogging {

  val serializer = Serializer.serializer;
  val PRIVATE = JsonViews.PRIVATE
  val PUBLIC = JsonViews.PUBLIC
  @Inject
  var authService: AuthenticationService = _
  @Inject
  var sessionService: SessionService = _

  def async[A] (block: SimpleResult)(implicit request: Request[A])  : Future[SimpleResult]= {
    Future{
      block
    }(AbstractController.context)
  }

  def notAsync[A] (block: SimpleResult)(implicit request: Request[A]) : Future[SimpleResult] = {
    Future.successful(block)
  }

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
      sessionService.getSession(request) match {
        case Right(request: AuthenticatedRequest[A]) =>
          logger.debug(s"Authenticated Request with user: ${request.user}")
          block(request)
        case Left(e: NoSessionError)      =>
          logger.debug("No session")
          Future.successful(Forbidden("No Session"))
        case Left(e: ExpiredSessionError) =>
          logger.debug("Expired Session")
          Future.successful(Forbidden("Expired Session"))
        case Left(other: Throwable)       => throw other
      }
    }

    def text(block: AuthenticatedRequest[String] => Future[SimpleResult]) = Action.async(parse.tolerantText) {
      request: Request[String] =>
        invokeBlock(request, block)
    }
    def json (block: AuthenticatedRequest[JsValue] => Future[SimpleResult]) = Action.async(parse.json) {
      request: Request[JsValue] =>
        invokeBlock(request, block)
    }
  }


  def validate[A, B](obj : A, validator: Validator)(implicit request: Request[B]): Either[SimpleResult, A] = {
    val bind = new DataBinder(obj)
    bind.setValidator(validator)
    bind.validate()
    val res = bind.getBindingResult
    if (res.hasErrors) {
      val globalError = res.getGlobalError
      val fieldErrors = res.getFieldErrors

      val response = Map(
        "errors" -> Map (
          "globalError" -> globalError,
          "fieldErrors" -> fieldErrors
        )
      )

      Left(JsonResponse.bad(response))
    }
    else {
      Right(obj)
    }
  }

  def skipAsync[T](result: T): Future[T] = Future.successful(result)
}
