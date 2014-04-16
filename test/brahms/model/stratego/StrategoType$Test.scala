package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoTypes._

class StrategoType$Test extends FunSuite {

  test("death") {
    val blueGeneral = new BluePiece(GENERAL_9)

    val redMarshal = new RedPiece(MARSHAL_10)
    val blueSpy = new BluePiece(SPY_1)
    val blueMiner = new BluePiece(MINER_3)
    val redBomb = new RedPiece(BOMB_11)
    val blueMarshal = new BluePiece(MARSHAL_10)
    val blueFlag = new BluePiece(FLAG_12)
    val redScout = new RedPiece(SCOUT_2)

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
