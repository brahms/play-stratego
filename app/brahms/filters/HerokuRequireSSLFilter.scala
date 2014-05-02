package brahms.filters

import play.api.mvc.{Results, SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future

object HerokuRequireSSLFilter extends Filter {
  override def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    val isSecure = rh.headers.get("x-forwarded-proto").map(_.equalsIgnoreCase("https")).getOrElse(true)

    if (isSecure) {
      f(rh)
    }
    else {
      Future.successful(Results.Forbidden("Please connect with SSL"))
    }
  }
}
