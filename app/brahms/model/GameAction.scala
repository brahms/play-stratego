package brahms.model

import scala.beans.BeanProperty

trait GameAction {
  @BeanProperty
  var actionId: Int = _

  def withActionId[C <: GameAction](id: Int): C = {
    setActionId(id)
    this.asInstanceOf[C]
  }
}
