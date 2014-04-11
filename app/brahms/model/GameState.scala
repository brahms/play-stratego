package brahms.model

import scala.Enumeration

object GameState extends Enumeration {
  type GameState = Value
  val PENDING = Value("PENDING")
  val RUNNING = Value("RUNNING")
  val FINISHED = Value("FINISHED")
}
