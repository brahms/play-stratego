package brahms.filters
import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import play.api.{Logger, Routes}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LoggingFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[SimpleResult]
             )(requestHeader: RequestHeader): Future[SimpleResult] = {
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result =>
      if(requestHeader.tags.contains(Routes.ROUTE_CONTROLLER)) {
        val action = requestHeader.tags(Routes.ROUTE_CONTROLLER) +
          "." + requestHeader.tags(Routes.ROUTE_ACTION_METHOD)
        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime
        Logger.info(s"${requestHeader.method} ${requestHeader.uri} thru ${action} took ${requestTime}ms" +
          s" and returned ${result.header.status}")
        result.withHeaders("Request-Time" -> requestTime.toString)

      }
      else
        result
    }
  }
}