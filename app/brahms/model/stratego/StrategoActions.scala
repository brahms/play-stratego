package brahms.model.stratego

import brahms.model.stratego.StrategoTypes._
import brahms.model.{Game, GameState, User}
import brahms.model.stratego.DeathType._
import scala.beans.BeanProperty

object StrategoActions {

  /**
   * Moves a piece from one x,y to another x,y
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class MoveAction(x: Int,
                        y: Int,
                        newX: Int,
                        newY: Int) extends StrategoAction {
    override def isLegal(game: Game): Boolean = {
      logger.trace(s"$this checking isLegal")
      game match {
        case game: StrategoGame =>
          if (outOfBounds(x, y)) {
            logger.debug(s"$this Illegal because outofbounds x,y")
            return false
          }
          if (outOfBounds(newX, newY)) {
            logger.debug(s"$this Illegal because outofbounds newX,newY")
            return false
          }
          if (game.phase != StrategoPhase.RUNNING) {
            logger.debug(s"$this Illegal because phase ${game.phase}")
            return false
          }
          val moveType = game.board(newX)(newY)
          game.board(x)(y) match {
            case piece: StrategoPiece  =>
              if (!game.sameUser(user, piece)) {
                logger.debug(s"$this Illegal because spoofing other player")
                return false
              }
              if (moveType != Empty()) {
                logger.debug(s"$this Illegal because move type is not empty type")
                return false

              }
              if (isDiagonal(x, y, newX, newY)) {
                logger.debug(s"$this Illegal because diagonal")
                return false
              }
              if (!game.currentPlayer.equals(user)) {
                logger.debug(s"$this Illegal because double move by $user")
                return false;
              }
              val dist = distance(x, y, newX, newY)
              if (dist > 1 && piece.value != SCOUT_2) {

                logger.debug(s"$this Illegal because dist > 1 and piece.value = ${piece.value}")
                return false
              }
              if (game.boundaryInPath(x, y, newX, newY)) {
                logger.debug(s"$this Illegal because boundaryInPath")
                return false
              }
              true
            case typ =>
              logger.debug(s"$this Illegal because not a stratego piece: $typ")
              false
          }
      }

    }

    override def invoke(game: Game): Unit = {
      game match {
        case game: StrategoGame =>
          game.movePiece(x, y, newX, newY)
          game.swapPlayer
          logger.trace(s"$user moves ${game.getPiece(newX, newY)} from $x, $y to $newX, $newY")
          super.invoke(game)

      }
    }
  }

  /**
   * Attacks a piece
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class AttackAction(   x: Int, y: Int,
                           newX: Int,
                           newY: Int) extends StrategoAction {
    @BeanProperty
    var attacker: StrategoPiece = null
    @BeanProperty
    var defender: StrategoPiece = null
    @BeanProperty
    var result: DeathType = null
    override def invoke(game: Game): Unit = {
      game match {
        case game: StrategoGame =>

          attacker = game.board(x)(y).asInstanceOf[StrategoPiece]

          defender = game.board(newX)(newY).asInstanceOf[StrategoPiece]
          result = defender.ifAttackedBy(attacker)
          logger.trace(s"$user has $attacker attacking $defender and $result")
          var killedFlag: Boolean = false
          result match {
            case BOTH_DIE =>
              game.killPiece(x, y)
              game.killPiece(newX, newY)
            case ATTACKER_DIES =>
              game.killPiece(x, y)
            case DEFENDER_DIES =>
              killedFlag = game.killPiece(newX, newY)
              game.movePiece(x, y, newX, newY)
          }

          game.swapPlayer
          super.invoke(game)

          if (killedFlag) {
            val action = WinAction(WinReason.CAPTURED_FLAG).withUser(user)
            action.invoke(game)
          }
          else {

            val redCannotMove = !game.checkPlayerCanMove(true)
            val blueCannotMove = !game.checkPlayerCanMove(false)
            if (redCannotMove && blueCannotMove) {
              logger.debug("Both red and blue cannot move")
              val action = DrawAction(DrawReason.BOTH_PLAYERS_CANNOT_MOVE).withUser(game.redPlayer)
              action.invoke(game)
            }
            else if (redCannotMove) {
              logger.debug("Red cannot move")
              val action = WinAction(WinReason.OPPONENT_CANT_MOVE).withUser(game.redPlayer)
              action.invoke(game)
            }
            else if (blueCannotMove) {
              logger.debug("Blue cannot move")
              val action = WinAction(WinReason.OPPONENT_CANT_MOVE).withUser(game.bluePlayer)
              action.invoke(game)
            }
          }

      }
    }

    override def isLegal(game: Game): Boolean = {
      game match {
        case game: StrategoGame =>

          if (!game.currentPlayer.equals(user)) return false
          if (outOfBounds(x, y)) return false
          if (outOfBounds(newX, newY)) return false;
          if (game.phase != StrategoPhase.RUNNING) return false

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
  }

  /**
   * Places a piece for a user from the sideboard to the board
   * @param x
   * @param y
   * @param piece
   */
  case class PlacePieceAction(      x: Int,
                              y: Int,
                              piece: StrategoPiece)
    extends StrategoAction {
    override def invoke(game: Game): Unit = {
      game match {
        case game: StrategoGame =>

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
            case _: BluePiece =>
              game.getBlueSideboardFor(piece.value).remove(0)
              size = game.getBlueSideboardFor(piece.value).size
            case _: RedPiece =>
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
    }

    override def mask(user: User) = {
      if(this.user.equals(user))
        this
      else
        PlacePieceAction(x, y, piece.mask).withActionId(getActionId).withUser(user).asInstanceOf[PlacePieceAction]
    }

    override def isLegal(game: Game): Boolean = {
      game match {
        case game: StrategoGame =>

          if (outOfBounds(x, y)) return false
          if (game.phase != StrategoPhase.PLACE_PIECES) return false;
          if (!game.sameUser(user, piece)) return false;
          if (Empty() != game.board(x)(y)) return false;
          if (!piece.isValid) return false
          piece match {
            case piece: BluePiece =>
              if (!game.isOnBlueSide(x, y))
                return false
              if (!game.stillInSideboard(piece))
                return false
              true
            case piece: RedPiece =>
              if (!game.isOnRedSide(x, y))
                return false
              if (!game.stillInSideboard(piece))
                return false
              true
          }
      }
    }
  }

  /**
   * Moves a already placed piece somewhere else
   * @param x
   * @param y
   * @param newX
   * @param newY
   */
  case class ReplacePieceAction(x: Int,
                                y: Int,
                                newX: Int,
                                newY: Int)
    extends StrategoAction {
    override def invoke(game: Game): Unit = {
      game match {
        case game: StrategoGame =>

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
    }

    override def isLegal(game: Game): Boolean = {
      game match {
        case game: StrategoGame =>

          if (outOfBounds(x, y)) return illegal("out of bounds old pos")
          if (outOfBounds(newX, newY)) return illegal("out of bounds new pos")
          if (game.phase != StrategoPhase.PLACE_PIECES) return illegal("not in the place pieces phase")
          if (game.state != GameState.RUNNING) return illegal("not in the running state")
          game.board(x)(y) match {
            case piece: StrategoPiece =>
              if (!game.sameUser(user, piece))
                return illegal("not same user")
              piece match {
                case piece: BluePiece =>
                  if (!game.isOnBlueSide(newX, newY)) return illegal("not on blue side")

                  true
                case piece: RedPiece =>
                  if (!game.isOnRedSide(newX, newY)) return illegal("not on red side")

                  true
              }

            case _ =>
              false
          }
      }
    }

  }

  case class CommitAction() extends StrategoAction {
    /**
     * Checks if this commit action is legal, meaning the player isn't already commited
     * and their pieces have all been placed
     * @param game
     * @return
     */
    override def isLegal(game: Game): Boolean = {
      game match {
        case game: StrategoGame =>

          if (game.phase != StrategoPhase.PLACE_PIECES) {
            return illegal("Failing on clause 1, game phase is: " + game.phase)
          }
          if (game.bluePlayer.equals(user) && (game.bluePlayerReady || !game.blueSideboard.forall(_.isEmpty))) {
            return illegal(s"Failing on clause 2, bluePlayerEqualsUser: ${game.bluePlayer.equals(user)} bluePlayerReady: ${game.bluePlayerReady} !game.blueSideboard.forall(_.isEmpty)): ${!game.blueSideboard.forall(_.isEmpty)}")
          }
          if (game.redPlayer.equals(user) && (game.redPlayerReady || !game.redSideboard.forall(_.isEmpty))) {
            return illegal(s"Failing on clause 3, redPlayerEqualsUser: ${game.redPlayer.equals(user)} redPlayerReady: ${game.redPlayerReady} !game.redSideboard.forall(_.isEmpty)): ${!game.redSideboard.forall(_.isEmpty)}")
          }
          true
      }

    }

    override def invoke(game: Game): Unit = {

      game match {
        case game: StrategoGame =>

          if (game.bluePlayer.equals(user)) {
            logger.debug("Setting bluePlayer ready")
            game.bluePlayerReady = true
          }
          else {
            logger.debug("Setting redPlayer ready")
            game.redPlayerReady = true
          }

          if (game.redPlayerReady && game.bluePlayerReady)
            game.phase = StrategoPhase.RUNNING
          logger.trace(s"$user commits his piece placement")
          super.invoke(game)
      }
    }
  }

  case class WinAction(reason: WinReason) extends StrategoAction {
    override def isLegal(game: Game): Boolean =
      illegal(s"shouldn't be able to pass it up")


    override def invoke(game: Game): Unit = {
      game match {
        case game: StrategoGame =>
          logger.info(s"$user has won the StrategoGame due to $reason!")
          game.setWinningPlayer(Some(user.toSimpleUser))
          if (game.redPlayer.equals(user)) {
            game.setLosingPlayer(Some(game.bluePlayer))
          }
          else {
            game.setLosingPlayer(Some(game.redPlayer))
          }

          game.setState(GameState.FINISHED)
          super.invoke(game)
      }
    }
  }

  case class DrawAction(reason: DrawReason) extends StrategoAction {
    override def isLegal(game: Game): Boolean = illegal("cannot pass up a draw action")

    override def invoke(game: Game): Unit = {
      logger.info(s"Game has drawn due to $reason")
      game.state = GameState.FINISHED
      super.invoke(game)
    }
  }

}
