package brahms.model

import scala.beans.BeanProperty
import org.jongo.marshall.jackson.oid.{ObjectId, Id}
import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoGame
import scala.concurrent.duration._
import scala.collection.mutable
import org.jongo.marshall.jackson.oid
import org.joda.time.DateTime

object Game {
  val TIMEOUT = (60 seconds).toMillis
}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(new Type(value = classOf[StrategoGame], name = "Stratego")))
abstract class Game {
  def getType: String

  @Id
  @ObjectId
  var id : String = _;

  var finishedDate: DateTime = _

  def setId(id: String) : Unit = this.id = id
  def getId = id

  @BeanProperty
  var state: GameState = GameState.PENDING

  @BeanProperty
  var creator: User = _

  def mask(user: User) : Game

  def stateToString: String

  /**
   * When the game is over, this should return the players who won, lost and drawed
   * @return
   */
  def getGameStatistics: GameStats

  /**
   * A map of when a users to when they should timeout
   */
  @BeanProperty
  var timeouts: Map[String, Long] = Map()

  @BeanProperty
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
