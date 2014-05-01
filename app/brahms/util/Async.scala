package brahms.util

import scala.concurrent.{Future, ExecutionContext}
import java.util.concurrent.Executors
import play.api.mvc.{Request, SimpleResult}

object Async {
  val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
}

trait Async {
  def async[A] (block: A) : Future[A]= {
    Future{
      block
    }(Async.context)
  }

  def notAsync[A] (block: A) : Future[A] = {
    Future.successful(block)
  }
}
