package brahms.model

import scala.beans.BeanProperty
import org.jongo.marshall.jackson.oid.Id
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoGame
import org.joda.time.DateTime
import scala.concurrent.duration._

object Game {
  val TIMEOUT = (60 seconds).toMillis
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

  def getActionList: Seq[GameAction[_]]

  def mask(user: User) : Game

  def stateToString: String

  /**
   * When the game is over, this should return the players who won, lost and drawed
   * @return
   */
  def gameStats: GameStats

  /**
   * A map of when a users to when they should timeout
   */
  var timeouts: Map[String, Long] = Map()
  var players: Seq[User] = Seq()

  /**
   * If the game manager detects a timeout, the game should take care of handling it
   * @param user
   */
  def handleTimeout(user: User): Unit

  /**
   * This should return true when the game is over
   * @return
   */
  def isGameOver: Boolean = state == GameState.FINISHED



}
