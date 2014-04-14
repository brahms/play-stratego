package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoType.{DeathType, RedPiece, BluePiece}

class StrategoType$Test extends FunSuite {

  test("death") {
    val blueGeneral = new BluePiece(StrategoType.GENERAL_9)

    val redMarshal = new RedPiece(StrategoType.MARSHAL_10)
    val blueSpy = new BluePiece(StrategoType.SPY_1)
    val blueMiner = new BluePiece(StrategoType.MINER_3)
    val redBomb = new RedPiece(StrategoType.BOMB_11)
    val blueMarshal = new BluePiece(StrategoType.MARSHAL_10)
    val blueFlag = new BluePiece(StrategoType.FLAG_12)
    val redScout = new RedPiece(StrategoType.SCOUT_2)

    assert(blueGeneral.ifAttackedBy(redMarshal) == DeathType.DEFENDER_DIES)
    assert(redMarshal.ifAttackedBy(blueSpy) == DeathType.DEFENDER_DIES)
    assert(redBomb.ifAttackedBy(blueMiner) == DeathType.DEFENDER_DIES)
    assert(redMarshal.ifAttackedBy(blueGeneral) == DeathType.ATTACKER_DIES)
    assert(redMarshal.ifAttackedBy(blueMarshal) == DeathType.BOTH_DIE)
    assert(redBomb.ifAttackedBy(blueMarshal) == DeathType.ATTACKER_DIES)
    assert(blueFlag.ifAttackedBy(redMarshal) == DeathType.DEFENDER_DIES)
    assert(blueSpy.ifAttackedBy(redScout) == DeathType.DEFENDER_DIES)

  }
}
