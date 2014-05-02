package brahms.filters

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object StrictTransportSecurityFilter extends Filter {
  override def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    f(rh).map {
      result =>
        result.withHeaders( "Strict-Transport-Security" -> "max-age=31536000")

    }
  }
}
