package brahms.model.stratego

import brahms.model.stratego.StrategoTypes._
import brahms.model.{GameState, User}
import brahms.model.stratego.DeathType._
import scala.beans.BeanProperty

object StrategoActions {

  /**
   * Moves a piece from one x,y to another x,y
   * @param user
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class MoveAction(user: User,
                        x: Int,
                        y: Int,
                        newX: Int,
                        newY: Int) extends StrategoAction {
    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false;
      if (game.strategoState != StrategoState.RUNNING) return false;


      val moveType = game.board(newX)(newY)
      game.board(x)(y) match {
        case piece: StrategoPiece if game.sameUser(user, piece) && moveType == Empty() =>
          if (isDiagonal(x, y, newX, newY)) return false
          val dist = distance(x, y, newX, newY)
          if (dist > 1 && piece.value != SCOUT_2) return false
          if (game.boundaryInPath(x, y, newX, newY)) return false
          true
        case _ =>
          false
      }

    }

    override def invoke(game: StrategoGame): Unit = {
      game.movePiece(x, y, newX, newY)
      game.swapPlayer
      logger.trace(s"$user moves ${game.getPiece(newX, newY)} from $x, $y to $newX, $newY")
      super.invoke(game)
    }
  }

  /**
   * Attacks a piece
   * @param user
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class AttackAction(user: User,
                           x: Int, y: Int,
                           newX: Int,
                           newY: Int) extends StrategoAction() {
    @BeanProperty
    var attacker: StrategoPiece = null
    @BeanProperty
    var defender: StrategoPiece = null
    @BeanProperty
    var result: DeathType = null
    override def invoke(game: StrategoGame): Unit = {
      attacker = game.board(x)(y).asInstanceOf[StrategoPiece]

      defender = game.board(newX)(newY).asInstanceOf[StrategoPiece]
      val resultEnum = defender.ifAttackedBy(attacker)
      logger.trace(s"$user has $attacker attacking $defender and $result")
      resultEnum match {
        case BOTH_DIE =>
          game.killPiece(x, y)
          game.killPiece(newX, newY)
        case ATTACKER_DIES =>
          game.killPiece(x, y)
        case DEFENDER_DIES =>
          val killedFlag = game.killPiece(newX, newY)
          game.movePiece(x, y, newX, newY)
          if(killedFlag) {
            logger.info(s"$user has won the StrategoGame!")
            game.state = GameState.FINISHED
            game.winningPlayer = Some(user)
          }
      }

      game.swapPlayer
      super.invoke(game)
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false;
      if (game.strategoState != StrategoState.RUNNING) return false;

      val defender = game.board(newX)(newY)
      val attacker = game.board(x)(y)


      attacker match {
        case piece: StrategoPiece if game.sameUser(user, piece) =>
          if (!isValidMove(x, y, newX, newY, piece)) return false;
          defender match {
            case attackedPiece: StrategoPiece if game.oppositeUser(user, attackedPiece) =>
              true
            case _ =>
              false
          }
        case _ =>
          false
      }
    }
  }

  /**
   * Places a piece for a user from the sideboard to the board
   * @param user
   * @param x
   * @param y
   * @param piece
   */
  case class PlacePieceAction(user: User,
                              x: Int,
                              y: Int,
                              piece: StrategoPiece)
    extends StrategoAction() {
    override def invoke(game: StrategoGame): Unit = {
      var putback: Option[StrategoPiece] = None
      game.board(x)(y) match {
        case replacedPiece: StrategoPiece =>
          logger.trace(s"Have to put back $replacedPiece")
          game.putbackIntoSideboard(replacedPiece)
          putback = Some(replacedPiece)
        case _ =>
      }

      game.board(x)(y) = piece
      var size = 0
      piece match {
        case _:BluePiece =>
          game.getBlueSideboardFor(piece.value).remove(0)
          size = game.getBlueSideboardFor(piece.value).size
        case _:RedPiece =>
          game.getRedSideboardFor(piece.value).remove(0)
          size = game.getRedSideboardFor(piece.value).size
      }
      var logstmt = s"$user places $piece at ($x, $y). Left in sideboard: $size. "
      putback.foreach {
        p => logstmt += s"Also put back $p to sideboard"
      }
      logger.trace(logstmt)
      super.invoke(game)
    }

    override def mask(user: User) = {
      if(this.user.equals(user))
        this
      else
        PlacePieceAction(this.user, x, y, piece.mask).withActionId(getActionId)
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (game.strategoState != StrategoState.PLACE_PIECES) return false;
      if (!game.sameUser(user, piece)) return false;
      if (Empty() != game.board(x)(y)) return false;
      if (!piece.isValid) return false
      piece match {
        case piece: BluePiece =>
          if (game.isOnRedSide(x, y))
            return false
          if (!game.stillInSideboard(piece))
            return false
          true
        case piece: RedPiece =>
          if (game.isOnBlueSide(x, y))
            return false
          if (!game.stillInSideboard(piece))
            return false
          true
      }
    }
  }

  /**
   * Moves a already placed piece somewhere else
   * @param user
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class ReplacePieceAction(user: User,
                                x: Int,
                                y: Int,
                                newX: Int,
                                newY: Int)
    extends StrategoAction() {
    override def invoke(game: StrategoGame): Unit = {
      var removedPiece: Option[StrategoPiece] = None
      game.board(newX)(newY) match {
        case piece: BluePiece =>
          game.putbackIntoSideboard(piece)
          removedPiece = Some(piece)
        case piece: RedPiece =>
          game.putbackIntoSideboard(piece)
          removedPiece = Some(piece)
        case _ =>
      }

      game.board(newX)(newY) = game.board(x)(y)
      game.board(x)(y) = Empty()
      var logstmt = s"$user replaces ${game.board(newX)(newY)} from $x, $y to ($newX, $newY). "
      removedPiece.foreach {
        p => logstmt += s"Also put $p back into sideboard"
      }
      logger.trace(logstmt)

      super.invoke(game)
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false
      if (game.strategoState != StrategoState.PLACE_PIECES) return false;
      game.board(x)(y) match {
        case piece: StrategoPiece =>
          if (!game.sameUser(user, piece))
            return false
          piece match {
            case piece: BluePiece =>
              if (!game.isOnBlueSide(newX, newY)) return false

              true
            case piece: RedPiece =>
              if (!game.isOnRedSide(newX, newY)) return false

              true
          }

        case _ =>
          false
      }
    }

  }

  case class CommitAction(user: User) extends StrategoAction() {
    /**
     * Checks if this commit action is legal, meaning the player isn't already commited
     * and their pieces have all been placed
     * @param game
     * @return
     */
    override def isLegal(game: StrategoGame): Boolean = {
      if (game.strategoState != StrategoState.PLACE_PIECES) {
        logger.debug("Failing on clause 1, game phase is: {}", game.strategoState)
        return false
      }
      if (game.bluePlayer.equals(user) && (game.bluePlayerReady || !game.blueSideboard.forall(_.isEmpty))) {
        logger.debug(s"Failing on clause 2, bluePlayerEqualsUser: ${game.bluePlayer.equals(user)} bluePlayerReady: ${game.bluePlayerReady } !game.blueSideboard.forall(_.isEmpty)): ${!game.blueSideboard.forall(_.isEmpty)}")
        return false
      }
      if (game.redPlayer.equals(user) && (game.redPlayerReady || !game.redSideboard.forall(_.isEmpty))) {
        logger.debug(s"Failing on clause 3, redPlayerEqualsUser: ${game.redPlayer.equals(user)} redPlayerReady: ${game.redPlayerReady } !game.redSideboard.forall(_.isEmpty)): ${!game.redSideboard.forall(_.isEmpty)}")

        return false
      }
      true
    }

    override def invoke(game: StrategoGame): Unit = {
      if (game.bluePlayer.equals(user))
        game.bluePlayerReady = true
      else
        game.redPlayerReady = true

      if (game.redPlayerReady && game.bluePlayerReady)
        game.strategoState = StrategoState.RUNNING
      logger.trace(s"$user commits his piece placement")
      super.invoke(game)

    }
  }

}
