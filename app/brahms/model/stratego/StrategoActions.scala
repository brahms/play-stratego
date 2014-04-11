package brahms.model.stratego

import brahms.model.stratego.StrategoType._
import brahms.model.User
import brahms.model.stratego.StrategoType.DeathType._
import brahms.model.stratego.StrategoGame.StrategoState

object StrategoActions {

  case class MoveAction(user: User,
                        x: Int,
                        y: Int,
                        newX: Int,
                        newY: Int) extends StrategoAction(user) {
    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false;
      if (game.strategoState != StrategoState.RUNNING) return false;


      val moveType = game.board(newX)(newY)
      game.board(x)(y) match {
        case piece: StrategoPiece if game.sameUser(user, piece) && moveType == Empty =>
          true
        case _ =>
          false
      }

    }

    override def invoke(game: StrategoGame): Unit = {
      game.movePiece(x, y, newX, newY)
      game.swapPlayer
    }
  }

  case class AttackAction(user: User,
                           x: Int, y: Int,
                           newX: Int,
                           newY: Int) extends StrategoAction(user) {
    override def invoke(game: StrategoGame): Unit = {
      val attacker = game.board(x)(y).asInstanceOf[StrategoPiece]

      val defender = game.board(newX)(newY).asInstanceOf[StrategoPiece]

      defender.ifAttackedBy(attacker) match {
        case BOTH_DIE =>
          game.killPiece(x, y)
          game.killPiece(newX, newY)
        case ATTACKER_DIES =>
          game.killPiece(x, y)
        case DEFENDER_DIES =>
          game.killPiece(newX, newY)
          game.movePiece(x, y, newX, newY)
      }
      game.swapPlayer
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false;
      if (game.strategoState == StrategoState.PLACE_PIECES) return false;

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

  case class PlacePieceAction(user: User,
                              x: Int,
                              y: Int,
                              piece: StrategoPiece)
    extends StrategoAction(user) {
    override def invoke(game: StrategoGame): Unit = {
      game.board(x)(y) = piece
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (game.strategoState == StrategoState.RUNNING) return false;
      if (!game.sameUser(user, piece)) return false;
      if (Empty != game.board(x)(y)) return false;
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

  case class ReplacePieceAction(user: User,
                                x: Int,
                                y: Int,
                                newX: Int,
                                newY: Int)
    extends StrategoAction(user) {
    override def invoke(game: StrategoGame): Unit = {
      game.board(newX)(newY) match {
        case piece: BluePiece =>
          game.putbackIntoSideboard(piece)
        case piece: RedPiece =>
          game.putbackIntoSideboard(piece)
        case _ =>
      }

      game.board(newX)(newY) = game.board(x)(y)
      game.board(x)(y) = Empty
    }

    override def isLegal(game: StrategoGame): Boolean = {
      if (outOfBounds(x, y)) return false
      if (outOfBounds(newX, newY)) return false
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

  case class CommitAction(user: User) extends StrategoAction(user) {
    override def isLegal(game: StrategoGame): Boolean = {
      if (game.strategoState != StrategoState.PLACE_PIECES)
        return false
      if (game.bluePlayer.equals(user) && game.bluePlayerReady)
        return false
      if (game.redPlayer.equals(user) && game.redPlayerReady)
        return false

      true
    }

    override def invoke(game: StrategoGame): Unit = {
      if (game.bluePlayer.equals(user))
        game.bluePlayerReady = true
      else
        game.redPlayerReady = true

      if (game.redPlayerReady && game.bluePlayerReady)
        game.strategoState = StrategoState.RUNNING

    }
  }

}
