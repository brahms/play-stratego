package brahms.model

import scala.beans.BeanProperty

trait GameAction {
  @BeanProperty
  var actionId: Int = _

  @BeanProperty
  var user: User = _

  def isLegal(game: Game) : Boolean
  def invoke(game: Game) : Unit

  def withActionId(id: Int): GameAction = {
    setActionId(id)
    this
  }
  def withUser(user: User): GameAction = {
    setUser(user)
    this
  }

  def mask(user: User): GameAction = {
    this
  }
}
