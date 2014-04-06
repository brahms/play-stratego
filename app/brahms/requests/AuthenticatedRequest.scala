package brahms.requests

import brahms.model.User
import play.api.mvc.{WrappedRequest, Request}

case class AuthenticatedRequest[A](user: User, token: String, request: Request[A]) extends WrappedRequest(request)
