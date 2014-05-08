package brahms.model.stratego

import brahms.model._
import scala.beans.{BooleanBeanProperty, BeanProperty}
import scala.collection.mutable
import brahms.model.stratego.StrategoTypes._
import scala.reflect.ClassTag
import scala.util.control.Breaks._
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonView}
import brahms.serializer.{Serializer, JsonViews}
import brahms.model.stratego.StrategoTypes.Boundary
import brahms.model.stratego.StrategoTypes.Empty
import brahms.model.stratego.StrategoActions.{WinAction, DrawAction}
import java.beans.Transient
import org.slf4j.LoggerFactory
import scala.concurrent.duration._


/**
 * Encapsulates the current state of the board
 */
class StrategoGame extends Game {

  @JsonIgnore
  @Transient
  val logger = LoggerFactory.getLogger(getClass)
  /**
   * Our board is really just a 2d matrix of StrategoTypes
   */
  type Board = Array[Array[StrategoType]]


  @BeanProperty
  var actionList: mutable.Buffer[StrategoAction] = new mutable.ListBuffer[StrategoAction]

  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var board: Board = _

  /**
   * The blue side board is where blue pieces start the game, or end up after death
   */
  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var blueSideboard: Array[mutable.ArrayBuffer[BluePiece]] = _

  /**
   * The red side board is where red pieces start the game or end up after death
   */
  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var redSideboard: Array[mutable.ArrayBuffer[RedPiece]] = _

  /**
   * The red player, the player who started the game, and the player who goes first
   */
  @BeanProperty
  var redPlayer: User = _

  /**
   * The blue player, the player who goes second
   */
  @BeanProperty
  var bluePlayer: User = _

  /**
   * The current turns player
   */
  @BeanProperty
  var currentPlayer: User = _

  @BeanProperty
  var winningPlayer: Option[User] = _

  @BeanProperty
  var losingPlayer: Option[User] = _

  /**
   * True when the blue player has sent a commit action
   */
  @BooleanBeanProperty
  var bluePlayerReady: Boolean = false

  /**
   * True when the red player has sent a commit action
   */
  @BooleanBeanProperty
  var redPlayerReady: Boolean = false

  /**
   * The current state of the game, either PLACE_PIECES OR RUNNING
   */
  @BeanProperty
  var phase: StrategoPhase = StrategoPhase.PLACE_PIECES



  /**
   * Swaps the currentPlayer (basically ending a turn)
   */
  def swapPlayer {
    if (currentPlayer.equals(bluePlayer))
      currentPlayer = redPlayer
    else
      currentPlayer = bluePlayer
  }

  /**
   * Returns the piece at the given x,y coord
   * @param x
   * @param y
   * @return
   */
  def getPiece(x: Int, y: Int) = {
    board(x)(y)
  }

  /**
   * Places a piece arbitrarily on the board
   * @param x
   * @param y
   * @param piece
   */
  def setPiece(x: Int, y: Int, piece: StrategoType) {
    assert(board(x)(y) != Boundary())
    board(x)(y) = piece
  }

  /**
   * Moves a piece from an x,y coord to a new position
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  def movePiece(x: Int, y: Int, newX: Int, newY: Int) {
    assert(board(x)(y) != Boundary())
    assert(board(newX)(newY) == Empty())
    logger.trace(s"Moving piece: ${board(x)(y)} from $x,$y to $newX,$newY")
    board(newX)(newY) = board(x)(y)
    board(x)(y) = Empty()
  }

  /**
   * Kills a piece, removing it from the x,y coord and putting it back in the sideboard, returns true if flag was killed
   * @param x
   * @param y
   */
  def killPiece(x: Int, y: Int) : Boolean = {
    assert(board(x)(y).isInstanceOf[StrategoPiece])
    logger.trace(s"Killing piece: ${board(x)(y)} at $x,$y")
    var killedFlag = false
    board(x)(y) match {
      case piece: RedPiece =>
        if (piece.getValue == StrategoTypes.FLAG_12) killedFlag = true
        getRedSideboardFor(piece.value) += piece
      case piece: BluePiece =>
        if (piece.getValue == StrategoTypes.FLAG_12) killedFlag = true
        getBlueSideboardFor(piece.value) += piece
      case piece =>
        throw new IllegalArgumentException("Bad piece to kill: " + piece)
    }
    board(x)(y) = Empty()
    killedFlag
  }


  def getRedSideboardFor(value: Int) = {
    redSideboard(value-1)
  }
  def getBlueSideboardFor(value: Int) = {
    blueSideboard(value-1)
  }

  /**
   * Given a value, returns the right amount of pieces to put into the sideboard
   * @param value
   * @param tag
   * @tparam T
   * @return
   */
  private def initSideBoard[T <: StrategoPiece](value: Int)(implicit tag: ClassTag[T]): mutable.ArrayBuffer[T] = {
    val pieces = new mutable.ArrayBuffer[T]

    val num: Int = value match {
      case StrategoTypes.BOMB_11 => 6
      case StrategoTypes.FLAG_12 => 1
      case StrategoTypes.SPY_1 => 1
      case StrategoTypes.SCOUT_2 => 8
      case StrategoTypes.MINER_3 => 5
      case StrategoTypes.SERGENT_4 => 4
      case StrategoTypes.LIEUTENANT_5 => 4
      case StrategoTypes.CAPTAIN_6 => 4
      case StrategoTypes.MAJOR_7 => 3
      case StrategoTypes.COLONEL_8 => 2
      case StrategoTypes.GENERAL_9 => 1
      case StrategoTypes.MARSHAL_10 => 1
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

  /**
   * Sets up the boundaries of the game, including the edges and the lakes
   * @param board
   */
  private def initBoundaries(board: Board) = {
    (0 to 11).foreach {
      i =>
        // set top boundary
        board(i)(0) = Boundary()
        //bottom
        board(i)(11) = Boundary()
        //left side
        board(0)(i) = Boundary()
        //right side
        board(11)(i) = Boundary()
    }

    //lakes
    board(3)(5) = Boundary()
    board(3)(6) = Boundary()
    board(4)(5) = Boundary()
    board(4)(6) = Boundary()

    //lakes
    board(7)(5) = Boundary()
    board(7)(6) = Boundary()
    board(8)(5) = Boundary()
    board(8)(6) = Boundary()
    val time = System.currentTimeMillis()
    timeouts = Map(
      redPlayer.username -> (time + (10 minutes).toMillis),
      bluePlayer.username -> (time + (10 minutes).toMillis)
    )
  }

  /**
   * Returns true if there's a boundary between x,y and newX, newY
   * @param x
   * @param y
   * @param newX
   * @param newY
   * @return
   */
  def boundaryInPath(x: Int, y: Int, newX: Int, newY: Int): Boolean = {
    val byX = if (x < newX) 1 else -1
    val byY = if (y < newY) 1 else -1
    var yes = false;
    breakable {
      for (checkX <- (x to newX by byX); checkY <- (y to newY by byY)) {
        if (board(checkX)(checkY) == Boundary()) {
          yes = true
          break
        }
      }
    }

    yes
  }

  /**
   * Initializes the game, making it ready to played
   */
  def init {
    assert(redPlayer != null)
    assert(bluePlayer != null)
    currentPlayer = redPlayer
    board = Array.fill[StrategoType](12, 12)(Empty())
    initBoundaries(board)
    setState(GameState.RUNNING)
    blueSideboard = Array.ofDim(12)
    redSideboard = Array.ofDim(12)
    (MINVAL to MAXVAL).foreach {
      i =>
        blueSideboard(i - 1) = initSideBoard[BluePiece](i)
        redSideboard(i - 1) = initSideBoard[RedPiece](i)
    }


  }
  override def toString: String =
    s"StrategoGame[id: $id, " +
      s"state: $state, " +
      s"phase: $phase, " +
      s"redPlayer: $redPlayer, " +
      s"bluePlayer: $bluePlayer, " +
      s"currentPlayer: $currentPlayer" +
      s"]"

  /**
   * Returns a short hand form of the given board state, and the sideboard states
   * @return
   */
  override def stateToString: String = {
    val b = new mutable.StringBuilder
    b.append(s"Game id: $id")
    b.append(s"\nGame state: $state")
    b.append(s"\nGame phase: $phase")
    b.append(s"\nRed player: $redPlayer")
    b.append(s"${if(redPlayerReady) "(Not Ready)" else ""}")
    b.append(s"\nBlue player: $bluePlayer")
    b.append(s"${if(bluePlayerReady) "(Not Ready)" else ""}")
    b.append(s"\nPlayers turn: $currentPlayer")
    if (actionList.nonEmpty)
      b.append(s"\nLast Action: ${Serializer.serializer.writeValueAsString(actionList.last)}")
    if (state == GameState.RUNNING) {
      b.append("\n\n")
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
    }
    b.append("\n")
    b.toString()
  }

  /**
   * Returns true if the user controls the given piece
   * @param user
   * @param piece
   * @return
   */
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

  /**
   * Returns true if the opposite player controls the given piece
   * @param user
   * @param piece
   * @return
   */
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


  /**
   * Returns true if the piece is still in the sideboard
   * @param piece
   * @return
   */
  def stillInSideboard(piece: StrategoPiece) = {
    piece match {
      case piece: RedPiece =>
        !getRedSideboardFor(piece.value).isEmpty
      case piece: BluePiece =>
        !getBlueSideboardFor(piece.value).isEmpty

    }
  }

  /**
   * Puts a piece back into the sideboard
   * @param piece
   */
  def putbackIntoSideboard(piece: StrategoPiece) {
    assert(piece.value != UNKNOWN_13)
    piece match {
      case piece: RedPiece =>
        getRedSideboardFor(piece.value) += piece
      case piece: BluePiece =>
        getBlueSideboardFor(piece.value) += piece

    }
  }

  /**
   * Removes a piece from the sideboard
   * @param piece
   */
  def removeFromSideboard(piece: StrategoPiece) {
    assert(piece.value != UNKNOWN_13)
    piece match {
      case piece: RedPiece =>
        getRedSideboardFor(piece.value).remove(0)
      case piece: BluePiece =>
        getBlueSideboardFor(piece.value).remove(0)
    }
  }

  /**
   * Returns true if the given x,y coord is on the red side of the board
   * @param x
   * @param y
   * @return
   */
  def isOnRedSide(x: Int, y: Int): Boolean = {
    if (board(x)(y) == Boundary()) return false
    if (y <= 4)
      true
    else
      false
  }

  /**
   * Returns true if the given x,y coord is on the blue side of the board
   * @param x
   * @param y
   * @return
   */
  def isOnBlueSide(x: Int, y: Int): Boolean = {
    if (board(x)(y) == Boundary()) return false
    if (y >= 7)
      true
    else
      false
  }

  def maskBoard(user: User): StrategoGame.this.type#Board = {
    board.map {
      row => row.map {
        element =>
          element match {
            case _:RedPiece if !user.equals(redPlayer) =>
              new RedPiece(UNKNOWN_13)
            case _:BluePiece if !user.equals(bluePlayer) =>
              new BluePiece(UNKNOWN_13)
            case _ =>
              element
          }
      }
    }
  }

  def mask(user: User):StrategoGame = {
    val game = new StrategoGame
    game.setBluePlayer(bluePlayer)
    game.setRedPlayer(redPlayer)
    game.setCurrentPlayer(currentPlayer)
    game.setBoard(maskBoard(user))
    game.actionList = actionList.map(_.mask(user).asInstanceOf[StrategoAction])
    game.setPhase(getPhase)
    game.setState(state)
    game.setId(id)
    game.players = players
    game
  }

  override def getType: String = "Stratego"

  /**
   * If the game manager detects a timeout, the game should take care of handling it
   * @param user
   */
  override def handleTimeout(user: User): Unit = {
    getState match {
      case GameState.PENDING =>
        logger.error("Timeout for stratego game in the pending state {}", this)
      case GameState.RUNNING =>
        losingPlayer = Some(user)
        if (losingPlayer.equals(redPlayer)) {
          winningPlayer = Some(bluePlayer)
        }
        else {
          winningPlayer = Some(redPlayer)
        }
        logger.debug(s"Timeout for $losingPlayer, forfeits. $winningPlayer wins!")
        WinAction(WinReason.OPPONENT_TIMED_OUT).withUser(winningPlayer.get).invoke(this)
      case GameState.FINISHED =>
        logger.error("Handle timeout called when game was already finished")
      case GameState.CANCELED =>
        logger.error("Handle timeout called when game was already canceled")
    }

    setState(GameState.FINISHED)

  }

  /**
   * When the game is over, this should return the players who won, lost and drawed
   * @return
   */
  override def getGameStatistics: GameStats = {
    if (state == GameState.FINISHED) {
      actionList.last match {
        case WinAction(reason) =>
          logger.debug("Generating game stats for Win: " + reason)
          GameStats(
            players = Seq(redPlayer.toSimpleUser, bluePlayer.toSimpleUser),
            winners =  Seq(winningPlayer.get.toSimpleUser),
            losers = Seq(losingPlayer.get.toSimpleUser),
            game = this
          )
        case DrawAction(reason) =>
          logger.debug("Generating game stats for Draw: " + reason)
          GameStats(
            players = players,
            game = this,
            draws = players
          )
      }
    }
    else {
      throw new IllegalArgumentException("GameStats called on non finished game")
    }
  }


  def checkPlayerCanMove(checkRedPlayer: Boolean): Boolean = {
    (1 to 10).foreach { x =>
      (1 to 10).foreach { y=>
          val top = board(x)(y+1)
          val right = board(x+1)(y)
          val bottom = board(x)(y-1)
          val left = board(x-1)(y)
          board(x)(y) match {
            case piece: RedPiece if piece.isMoveable && checkRedPlayer=>
              if (top.isInstanceOf[BluePiece] || top.isInstanceOf[Empty] ||
                  right.isInstanceOf[BluePiece] || right.isInstanceOf[Empty] ||
                  bottom.isInstanceOf[BluePiece] || bottom.isInstanceOf[Empty] ||
                  left.isInstanceOf[BluePiece] || left.isInstanceOf[Empty]) {
                return true
              }
            case piece: BluePiece if piece.isMoveable && !checkRedPlayer=>
              if (top.isInstanceOf[RedPiece] || top.isInstanceOf[Empty] ||
                  right.isInstanceOf[RedPiece] || right.isInstanceOf[Empty] ||
                  bottom.isInstanceOf[RedPiece] || bottom.isInstanceOf[Empty] ||
                  left.isInstanceOf[RedPiece] || left.isInstanceOf[Empty]) {
                return true
              }
            case _ =>
          }
      }
    }
    false
  }

  def getOtherPlayer(user: User): User = {
    if (user.equals(redPlayer)) return bluePlayer
    redPlayer
  }
}
