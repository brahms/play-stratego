package brahms.actors

import javax.inject.{Inject, Named}
import akka.actor._
import org.springframework.context.ApplicationContext
import javax.annotation.PreDestroy

/**
 * An Akka Extension which holds the ApplicationContext for creating actors from bean templates.
 */
private object SpringExt extends ExtensionKey[SpringExt]
private class SpringExt(system: ExtendedActorSystem) extends Extension {
  @volatile var ctx: ApplicationContext = _
}

@Named
class ActorSystemBean @Inject() (ctx: ApplicationContext) {
  /**
   * Keep the ActorSystem private to retain control over which services it
   * provides to consumers of this bean.
   */
  private val system = ActorSystem("AkkaSpring")


  /**
   * This stores the ApplicationContext within the ActorSystem’s Spring
   * extension for later use; it also enables that child actors could be created
   * from bean templates (not currently demonstrated in this sample).
   */
  SpringExt(system).ctx = ctx

  @PreDestroy
  def shutdown(): Unit = system.shutdown()

  lazy val gameManager = system.actorOf(Props(SpringExt(system).ctx.getBean(classOf[GameManager])))

//  system.scheduler.schedule(
//    initialDelay = (1 minute),
//    interval = (30 seconds),
//    receiver = gameManager,
//    message = CheckTimeout
//  )(Async.context)

}
