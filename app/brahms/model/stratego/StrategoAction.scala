package brahms.model.stratego

import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
import scala.beans.BeanProperty
import brahms.model.{GameAction, User}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoTypes.StrategoPiece
import brahms.util.WithLogging

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes(Array(new Type(classOf[StrategoActions.AttackAction]),
  new Type(value=classOf[StrategoActions.MoveAction], name="MoveAction"),
  new Type(value=classOf[StrategoActions.PlacePieceAction], name="PlacePieceAction"),
  new Type(value=classOf[StrategoActions.AttackAction], name="AttackAction"),
  new Type(value=classOf[StrategoActions.ReplacePieceAction], name="ReplacePieceAction"),
  new Type(value=classOf[StrategoActions.CommitAction], name="CommitAction")))
abstract class StrategoAction extends GameAction[StrategoGame] with WithLogging {
  override def isLegal(game: StrategoGame): Boolean
  override def invoke(game: StrategoGame) : Unit = {
    game.actionList += this
    setActionId(game.actionList.size)
  }

  /**
   * Masks a action to be serialized to a specific user, usually this
   * means turning any values to the value UNKNOWN when serialized to the other player
   * @param user
   * @return
   */
  def mask(user: User) : StrategoAction = this


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
