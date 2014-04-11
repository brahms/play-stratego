package brahms.model

import org.springframework.data.annotation.Id
import scala.beans.BeanProperty
import brahms.model.GameState.GameState

object Game {
}

abstract class Game {

  @Id
  @BeanProperty
  var id : String = _;

  @BeanProperty
  var state: GameState = GameState.PENDING

  @BeanProperty
  var creator: User = _



}
