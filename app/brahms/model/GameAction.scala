package brahms.model

import scala.beans.BeanProperty

trait GameAction[T <: Game] {
  @BeanProperty
  var actionId: Int = _

  def isLegal(game: T) : Boolean
  def invoke(game: T) : Unit

  def withActionId[C <: GameAction[T]](id: Int): C = {
    setActionId(id)
    this.asInstanceOf[C]
  }
}
