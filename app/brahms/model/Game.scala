package brahms.model

import scala.beans.BeanProperty
import brahms.model.GameState.GameState
import org.jongo.marshall.jackson.oid.Id
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoGame

object Game {
}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(new Type(value = classOf[StrategoGame], name = "Stratego")))
abstract class Game {
  def getType: String


  @Id
  @BeanProperty
  var id : ObjectId = _;

  @BeanProperty
  var state: GameState = GameState.PENDING

  @BeanProperty
  var creator: User = _

  def mask(user: User) : Game




}
