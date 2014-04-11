package brahms.model.stratego

import brahms.model.{User, Game}
import scala.beans.{BooleanBeanProperty, BeanProperty}
import scala.collection.mutable
import brahms.model.stratego.StrategoType._
import scala.reflect.ClassTag
import brahms.model.stratego.StrategoGame.StrategoState.StrategoState
import brahms.util.WithLogging

object StrategoGame {

  object StrategoState extends Enumeration {
    type StrategoState = Value
    val PLACE_PIECES = Value("PLACE_PIECES")
    val RUNNING = Value("RUNNING")
  }

}

class StrategoGame extends Game with WithLogging {
  type Board = Array[Array[StrategoType]]

  @BeanProperty
  var board: Board = _

  @BeanProperty
  var blueSideboard: Array[mutable.ArrayBuffer[BluePiece]] = _

  @BeanProperty
  var redSideboard: Array[mutable.ArrayBuffer[RedPiece]] = _

  @BeanProperty
  var redPlayer: User = _

  @BeanProperty
  var bluePlayer: User = _

  @BeanProperty
  var currentPlayer: User = _

  @BooleanBeanProperty
  var bluePlayerReady: Boolean = false

  @BooleanBeanProperty
  var redPlayerReady: Boolean = false

  @BeanProperty
  var strategoState: StrategoState = _

  @BeanProperty
  var actionList: mutable.Buffer[StrategoAction] = new mutable.ListBuffer[StrategoAction]


  def swapPlayer {
    if (currentPlayer.equals(bluePlayer))
      currentPlayer = redPlayer
    else
      currentPlayer = bluePlayer
  }

  def getPiece(x: Int, y: Int) = {
    board(x)(y)
  }

  def setPiece(x: Int, y: Int, piece: StrategoType) {
    assert(board(x)(y) != Boundary)
    board(x)(y) = piece
  }

  def movePiece(x: Int, y: Int, newX: Int, newY: Int) {
    assert(board(x)(y) != Boundary)
    assert(board(newX)(newY) == Empty)
    board(newX)(newY) = board(x)(y)
    board(x)(y) = Empty
  }

  def killPiece(x: Int, y: Int) {
    assert(board(x)(y).isInstanceOf[StrategoPiece])

    board(x)(y) match {
      case piece: RedPiece =>
        redSideboard(piece.value) += piece
      case piece: BluePiece =>
        blueSideboard(piece.value) += piece
    }
  }

  private def initSideBoard[T <: StrategoPiece](value: Int)(implicit tag: ClassTag[T]): mutable.ArrayBuffer[T] = {
    val pieces = new mutable.ArrayBuffer[T]

    val num: Int = value match {
      case StrategoType.BOMB => 6
      case StrategoType.FLAG => 1
      case StrategoType.SPY => 1
      case StrategoType.SCOUT => 6
      case StrategoType.MINER => 5
      case StrategoType.SERGENT => 5
      case StrategoType.LIEUTENANT => 4
      case StrategoType.CAPTAIN => 4
      case StrategoType.MAJOR => 3
      case StrategoType.COLONEL => 2
      case StrategoType.GENERAL => 1
      case StrategoType.MARSHAL => 1
      case _ =>
        throw new IllegalArgumentException("Cannot convert: " + value)
    }

    val range = (0 until num)
    if (tag.runtimeClass.equals(classOf[BluePiece])) {
      range.foreach {
        _ => pieces += new BluePiece(value).asInstanceOf[T]
      }
    }
    else {
      range.foreach {
        _ => pieces += new RedPiece(value).asInstanceOf[T]
      }
    }

    pieces
  }

  private def initBoundaries(board: Board) = {
    (0 to 11).foreach {
      i =>
        // set top boundary
        board(i)(0) = Boundary
        //bottom
        board(i)(11) = Boundary
        //left side
        board(0)(i) = Boundary
        //right side
        board(11)(i) = Boundary
    }

    //lakes
    board(3)(5) = Boundary
    board(3)(6) = Boundary
    board(4)(5) = Boundary
    board(4)(6) = Boundary

    //lakes
    board(7)(5) = Boundary
    board(7)(6) = Boundary
    board(8)(5) = Boundary
    board(8)(6) = Boundary
  }


  def init {
    board = Array.fill[StrategoType](12, 12)(Empty)
    initBoundaries(board)

    blueSideboard = Array.ofDim(12)
    redSideboard = Array.ofDim(12)
    (MINVAL to MAXVAL).foreach {
      i =>
        blueSideboard(i - 1) = initSideBoard[BluePiece](i)
        redSideboard(i - 1) = initSideBoard[RedPiece](i)
    }


  }

  override def toString: String = {
    val b = new mutable.StringBuilder
    (11 to 0 by -1).foreach {
      y =>
        (0 to 11).foreach {
          x =>
            b.append(board(x)(y).toShortHand)
        }
        b.append("\n")
    }

    b.append("\nRed SideBoard: | ")

    (MINVAL to MAXVAL).foreach {
      f =>
        b.append(" " + new RedPiece(f).toShortHand + s" (${redSideboard(f - 1).size}) |")
    }

    b.append("\nBlu SideBoard: | ")
    (MINVAL to MAXVAL).foreach {
      f =>
        b.append(" " + new BluePiece(f).toShortHand + s" (${blueSideboard(f - 1).size}) |")
    }
    b.append("\n")
    b.toString()

  }

  def sameUser(user: User, piece: StrategoPiece): Boolean = {
    piece match {
      case _: BluePiece if user.equals(getBluePlayer) =>
        true
      case _ if user.equals(getRedPlayer) =>
        true
      case _ =>
        false
    }
  }

  def oppositeUser(user: User, piece: StrategoPiece): Boolean = {

    piece match {
      case _: RedPiece if user.equals(getBluePlayer) =>
        true
      case _ if user.equals(getRedPlayer) =>
        true
      case _ =>
        false
    }
  }


  def stillInSideboard(piece: StrategoPiece) = {
    piece match {
      case piece: RedPiece =>
        !redSideboard(piece.value).isEmpty
      case piece: BluePiece =>
        !blueSideboard(piece.value).isEmpty

    }
  }

  def putbackIntoSideboard(piece: StrategoPiece) {
    piece match {
      case piece: RedPiece =>
        redSideboard(piece.value) += piece
      case piece: BluePiece =>
        blueSideboard(piece.value) += piece

    }
  }

  def removeFromSideboard(piece: StrategoPiece) {
    piece match {
      case piece: RedPiece =>
        redSideboard(piece.value).remove(0)
      case piece: BluePiece =>
        blueSideboard(piece.value).remove(0)
    }
  }

  def isOnRedSide(x: Int, y: Int): Boolean = {
    if (board(x)(y) == Boundary) return false
    if (y <= 4)
      true
    else
      false
  }

  def isOnBlueSide(x: Int, y: Int): Boolean = {
    if (board(x)(y) == Boundary) return false
    if (y >= 7)
      true
    else
      false
  }
}
