package brahms.model.stratego

import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonSubTypes}
import scala.beans.BeanProperty
import brahms.model.User
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import brahms.model.stratego.StrategoType.StrategoPiece

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes(Array(new Type(classOf[StrategoActions.AttackAction]),
  new Type(value=classOf[StrategoActions.MoveAction], name="MoveAction"),
  new Type(value=classOf[StrategoActions.PlacePieceAction], name="PlacePieceAction"),
  new Type(value=classOf[StrategoActions.ReplacePieceAction], name="ReplacePieceAction"),
  new Type(value=classOf[StrategoActions.CommitAction], name="CommitAction")))
abstract class StrategoAction(@BeanProperty user: User) {
  def isLegal(game: StrategoGame): Boolean
  def invoke(game: StrategoGame) : Unit

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
    else if(piece.value != StrategoType.SCOUT && distance(x,y,newX,newY) >1)
      false
    else if (outOfBounds(x,y) || outOfBounds(newX,newY))
      false
    else
      true
  }

}
