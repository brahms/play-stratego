package brahms.model.stratego

import brahms.model.stratego.StrategoType.DeathType.DeathType
import brahms.util.WithLogging
import com.fasterxml.jackson.annotation.JsonIgnore

object StrategoType extends WithLogging {
  val SPY = 1
  val SCOUT = 2
  val MINER = 3
  val SERGENT = 4
  val LIEUTENANT = 5
  val CAPTAIN = 6
  val MAJOR = 7
  val COLONEL = 8
  val GENERAL = 9
  val MARSHAL = 10
  val BOMB  = 11
  val FLAG = 12
  val MINVAL = SPY
  val MAXVAL = FLAG

  object DeathType extends Enumeration {
    type DeathType = Value
    val BOTH_DIE = Value("BOTH_DIE")
    val ATTACKER_DIES = Value("ATTACKER_DIES")
    val DEFENDER_DIES = Value("DEFENDER_DIES")
  }


  object Boundary extends StrategoType {
    override def toShortHand: String = "..."
    override def toString: String = "Boundary"
  }

  object Empty extends StrategoType {

    override def toShortHand: String = "   "
    override def toString: String = "Empty"
  }


  abstract class StrategoPiece(val value: Int) extends StrategoType {
      override def toShortHand: String = value match {
        case value if value == FLAG =>
          s"${char}FL"
        case value if value == BOMB =>
          s"${char}BO"
        case value if value == SPY =>
          s"${char}SP"
        case value if value == MINER =>
          s"${char}MI"
        case value if value >= 10 =>
          s"$char$value"
        case _ =>
          s" $char$value"
    }

    @JsonIgnore
    def isValid: Boolean = {
      value match {
        case value if value > FLAG =>
          false
        case value if value < SPY =>
          false
        case _ =>
          true
      }
    }

    def ifAttackedBy(piece: StrategoPiece): DeathType = {
      if (piece.getClass == getClass) throw new IllegalArgumentException("Cannot attack same color")
      val deathType = piece.value match {
        case piece if piece == value =>
          DeathType.BOTH_DIE
        case SPY if value == MARSHAL =>
          DeathType.DEFENDER_DIES
        case MINER if value == BOMB =>
          DeathType.DEFENDER_DIES
        case _ if value == FLAG =>
          DeathType.DEFENDER_DIES
        case _ if value == BOMB =>
          DeathType.ATTACKER_DIES
        case piece if value < piece =>
          DeathType.DEFENDER_DIES
        case piece if value > piece =>
          DeathType.ATTACKER_DIES
      }

      logger.debug("If {} attacks {}, Death type: {}", piece, this, deathType.toString)
      deathType
    }


    override def toString: String = {
      val b = new StringBuilder
      char match {
        case 'r' =>
          b.append("Red " )
        case 'b' =>
          b.append("Blue ")
      }

      b.append(value match {
        case SPY => "SPY"
        case GENERAL => "GENERAL"
        case MINER => "MINER"
        case MARSHAL => "MARSHAL"
        case FLAG => "FLAG"
        case BOMB => "BOMB"
        case SCOUT => "SCOUT"
        case MAJOR => "MAJOR"
        case LIEUTENANT => "LIEUTENANT"
        case CAPTAIN => "CAPTAIN"
        case COLONEL => "COLONEL"
      })

      b.append(s" ($value)")
      b.toString()
    }

    def char: Char
  }

  class RedPiece(value: Int) extends StrategoPiece(value) {

    override def char = 'r'


  }

  class BluePiece(value: Int) extends StrategoPiece(value) {
    override def char = 'b'

  }
}

trait StrategoType {
  def toShortHand: String
}
