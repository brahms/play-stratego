package brahms.model.stratego

import brahms.util.WithLogging
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import scala.beans.BeanProperty


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(new Type(value = classOf[StrategoTypes.Empty], name = "Empty")
, new Type(value = classOf[StrategoTypes.Boundary], name = "Boundary")
, new Type(value = classOf[StrategoTypes.BluePiece], name = "BluePiece")
, new Type(value = classOf[StrategoTypes.RedPiece], name = "RedPiece")))
sealed trait StrategoType {
  def toShortHand: String
}



object StrategoTypes extends WithLogging {
  val SPY_1 = 1
  val SCOUT_2 = 2
  val MINER_3 = 3
  val SERGENT_4 = 4
  val LIEUTENANT_5 = 5
  val CAPTAIN_6 = 6
  val MAJOR_7 = 7
  val COLONEL_8 = 8
  val GENERAL_9 = 9
  val MARSHAL_10 = 10
  val BOMB_11 = 11
  val FLAG_12 = 12
  val UNKNOWN_13 = 13
  val MINVAL = SPY_1
  val MAXVAL = FLAG_12


  case class Boundary() extends StrategoType {
    override def toShortHand: String = "..."

    override def toString: String = "Boundary"
  }

  case class Empty() extends StrategoType {

    override def toShortHand: String = "   "

    override def toString: String = "Empty"
  }
  sealed abstract class StrategoPiece(constructorValue: Int) extends StrategoType {
    @BeanProperty
    var value: Int = constructorValue

    override def toShortHand: String = value match {
      case value if value == FLAG_12 =>
        s"${char}FL"
      case value if value == BOMB_11 =>
        s"${char}BO"
      case value if value == SPY_1 =>
        s"${char}SP"
      case value if value == MINER_3 =>
        s"${char}MI"
      case value if value >= 10 =>
        s"$char$value"
      case _ =>
        s" $char$value"
    }

    @JsonIgnore
    def isValid: Boolean = {
      value match {
        case value if value > FLAG_12 =>
          false
        case value if value < SPY_1 =>
          false
        case _ =>
          true
      }
    }


    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case piece: StrategoPiece if piece.getClass.equals(getClass) && piece.value == getValue =>
          true
        case _ =>
          false
      }
    }

    def ifAttackedBy(piece: StrategoPiece): DeathType = {
      if (piece.getClass == getClass) throw new IllegalArgumentException("Cannot attack same color")
      val deathType = piece.value match {
        case piece if piece == value =>
          DeathType.BOTH_DIE
        case SPY_1 if value == MARSHAL_10 =>
          DeathType.DEFENDER_DIES
        case MINER_3 if value == BOMB_11 =>
          DeathType.DEFENDER_DIES
        case _ if value == FLAG_12 =>
          DeathType.DEFENDER_DIES
        case _ if value == BOMB_11 =>
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
          b.append("Red ")
        case 'b' =>
          b.append("Blue ")
      }

      b.append(value match {
        case SPY_1 => "SPY"
        case SCOUT_2 => "SCOUT"
        case MINER_3 => "MINER"
        case SERGENT_4 => "SERGENT"
        case LIEUTENANT_5 => "LIEUTENANT"
        case CAPTAIN_6 => "CAPTAIN"
        case MAJOR_7 => "MAJOR"
        case COLONEL_8 => "COLONEL"
        case GENERAL_9 => "GENERAL"
        case MARSHAL_10 => "MARSHAL"
        case BOMB_11 => "BOMB"
        case FLAG_12 => "FLAG"
      })

      b.append(s" ($value)")
      b.toString()
    }

    def char: Char

    def mask: StrategoPiece
  }

  class RedPiece @JsonCreator()(value: Int) extends StrategoPiece(value) {

    override def char = 'r'

    override def mask = new RedPiece(StrategoTypes.UNKNOWN_13)


  }

  class BluePiece @JsonCreator()(value: Int) extends StrategoPiece(value) {
    override def char = 'b'

    override def mask = new BluePiece(StrategoTypes.UNKNOWN_13)

  }

}