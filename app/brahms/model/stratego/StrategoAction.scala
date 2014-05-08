package brahms.model.stratego

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonTypeInfo, JsonSubTypes}
import scala.beans.BeanProperty
import brahms.model.{Game, GameAction, User}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoTypes.StrategoPiece
import brahms.util.WithLogging
import scala.concurrent.duration._
import org.joda.time.DateTime

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes(Array(
  new Type(value=classOf[StrategoActions.MoveAction], name="MoveAction"),
  new Type(value=classOf[StrategoActions.PlacePieceAction], name="PlacePieceAction"),
  new Type(value=classOf[StrategoActions.AttackAction], name="AttackAction"),
  new Type(value=classOf[StrategoActions.ReplacePieceAction], name="ReplacePieceAction"),
  new Type(value=classOf[StrategoActions.WinAction], name="WinAction"),
  new Type(value=classOf[StrategoActions.DrawAction], name="DrawAction"),
  new Type(value=classOf[StrategoActions.CommitAction], name="CommitAction")))
abstract class StrategoAction extends GameAction with WithLogging {
  @JsonIgnore
  val TIMEOUT_DURATION = (1 minute).toMillis

  override def isLegal(game: Game): Boolean
  override def invoke(game: Game) : Unit = {
    game match {
      case game: StrategoGame =>
        game.actionList += this
        setActionId(game.actionList.size)

        if (game.phase == StrategoPhase.RUNNING) {
          val timeoutForOtherUser = (TIMEOUT_DURATION+System.currentTimeMillis())
          game.timeouts = Map(game.getOtherPlayer(user).username -> timeoutForOtherUser)
          logger.debug("Setting timeout for other player to : " + new DateTime(timeoutForOtherUser))
        }
      case _ =>
        throw new IllegalArgumentException("Invalid game passed into invoke")
    }
  }

  def illegal(reason: String): Boolean = {
    logger.warn(s"$this illegal move due to $reason")
    false
  }


  protected def outOfBounds(x: Int, y:Int): Boolean = {
    if (x > 11 || x < 1) true
    else if (y > 11 || y < 1) true
    else false
  }

  def isDiagonal(x: Int, y: Int, newX: Int, newY: Int) = {
    if (x != newX && y != newY)
      true
    else
      false
  }

  def distance(x: Int, y: Int, newX: Int, newY: Int) : Int  = {
    if (x==newX) {
      Math.abs(newY- y)
    }
    else if (y==newY) {
      Math.abs(newX-x)
    }
    else
      throw new IllegalArgumentException(s"$x,$y -> $newX, $newY illegal distance")
  }

  def isValidMove(x: Int, y: Int, newX: Int, newY: Int, piece: StrategoPiece) = {
    if (isDiagonal(x,y,newX,newY))
      false
    else if(piece.value != StrategoTypes.SCOUT_2 && distance(x,y,newX,newY) >1)
      false
    else if (outOfBounds(x,y) || outOfBounds(newX,newY))
      false
    else
      true
  }

}
