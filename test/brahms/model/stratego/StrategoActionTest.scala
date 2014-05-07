package brahms.model.stratego

import org.scalatest.{BeforeAndAfter, FunSuite}
import brahms.model.stratego.StrategoActions._
import brahms.model.{GameAction, User}
import brahms.model.stratego.StrategoTypes._
import brahms.serializer.Serializer
import brahms.model.stratego.StrategoActions.MoveAction
import brahms.model.stratego.StrategoActions.AttackAction
import brahms.model.stratego.StrategoActions.PlacePieceAction
import org.bson.types.ObjectId

class StrategoActionTest extends FunSuite with BeforeAndAfter {
  val serializer = Serializer.serializer
  var redUser: User = _
  var blueUser: User = _
  var game: StrategoGame = _

  before {

    redUser = new User
    redUser.setAdmin(false)
    redUser.setUsername("redUser")
    blueUser = new User
    blueUser.setAdmin(false)
    blueUser.setUsername("blueUser")
    game = new StrategoGame
    game.setRedPlayer(redUser)
    game.setBluePlayer(blueUser)
    game.init

  }
  test("ReplacePieceAction") {

    val piece = new BluePiece(MAJOR_7)
    val piece2 = new BluePiece(GENERAL_9)
    val placePieceAction = PlacePieceAction(1, 10 , piece).withUser(redUser.toSimpleUser)
    val placePieceAction2 = PlacePieceAction(2,10, piece2).withUser(redUser.toSimpleUser)
    placePieceAction.invoke(game)
    placePieceAction2.invoke(game)

    assertResult(piece)(game.getPiece(1,10))
    assertResult(piece2)(game.getPiece(2,10))

    var replacePieceAction = ReplacePieceAction(1, 10, 2, 10).withUser(redUser.toSimpleUser).asInstanceOf[ReplacePieceAction]

    val json = serializer.writeValueAsString(replacePieceAction)
    replacePieceAction = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[ReplacePieceAction]
    assertResult(1)(replacePieceAction.x)
    assertResult(10)(replacePieceAction.y)
    assertResult(2)(replacePieceAction.newX)
    assertResult(10)(replacePieceAction.newY)

    assert(replacePieceAction.isLegal(game))

    replacePieceAction.invoke(game)

    assertResult(Empty())(game.getPiece(1,10))
    assertResult(piece)(game.getPiece(2,10))

    val actions = List[StrategoAction](replacePieceAction)

    println(Serializer.serializer.writeValueAsString(actions.toArray))


  }
  test("PlacePieceAction") {
    val piece = new BluePiece(MAJOR_7)

    val placePieceAction = PlacePieceAction(1, 10 , piece).withUser(redUser.toSimpleUser).asInstanceOf[PlacePieceAction]
    val json = serializer.writeValueAsString(placePieceAction)
    val actualAction = serializer.readValue(json, classOf[StrategoAction])
    assert(actualAction.isInstanceOf[PlacePieceAction])
    val actualPlaceAction = actualAction.asInstanceOf[PlacePieceAction]
    assertResult(placePieceAction.user)(actualPlaceAction.user)
    assertResult(placePieceAction.x)(actualPlaceAction.x)
    assertResult(placePieceAction.y)(actualPlaceAction.y)
    assertResult(UNKNOWN_13)(actualPlaceAction.mask(blueUser).asInstanceOf[PlacePieceAction].piece.value)

    val expectedSize = game.getBlueSideboardFor(piece.value).size-1
    actualPlaceAction.invoke(game)
    assertResult(piece)(game.getPiece(1, 10))
    assertResult(expectedSize)(game.getBlueSideboardFor(piece.value).size)
    assertResult(1)(actualPlaceAction.getActionId)

    // can also double invoke, which should move back the old piece into the sideboard
    actualPlaceAction.invoke(game)
    actualPlaceAction.invoke(game)
    assertResult(piece)(game.getPiece(1, 10))
    assertResult(expectedSize)(game.getBlueSideboardFor(piece.value).size)
    assertResult(3)(actualPlaceAction.getActionId)



  }

  test("AttackAction") {
    game.setPhase(StrategoPhase.RUNNING)
    val attackActionToSerialize = AttackAction(1, 1, 1, 2).withUser(redUser.toSimpleUser).asInstanceOf[AttackAction]
    val json = serializer.writeValueAsString(attackActionToSerialize)
    println(json)
    val attackAction = serializer.readValue(json, classOf[StrategoAction])
    val redPiece = new RedPiece(LIEUTENANT_5)
    game.setPiece(1,1, redPiece)
    game.setPiece(1,2, new BluePiece(SPY_1))
    assert(attackAction.isLegal(game))
    attackAction.invoke(game)
    assertResult (Empty()) ( game.getPiece(1, 1) )
    assertResult (redPiece) ( game.getPiece(1,2) )
    attackAction.setActionId(1)
    val jsonToClient = serializer.writeValueAsString(attackAction)
    println(jsonToClient)

    val oneMoreTime = serializer.readValue(jsonToClient, classOf[StrategoAction])
    assert(oneMoreTime.isInstanceOf[AttackAction])


  }

  test("MoveAction") {
    val redCaptain = new RedPiece(CAPTAIN_6)
    val redScout = new RedPiece(SCOUT_2)
    game.setPiece(1,1, redCaptain)
    game.setPiece(2,1, redScout)
    game.setPhase(StrategoPhase.RUNNING)

    var captainMove = MoveAction(1, 1, 1, 2).withUser(redUser.toSimpleUser).asInstanceOf[MoveAction]
    var scoutMove = MoveAction(2, 1, 2, 4).withUser(redUser.toSimpleUser).asInstanceOf[MoveAction]

    var json = serializer.writeValueAsString(captainMove)
    captainMove = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[MoveAction]
    assertResult(1)(captainMove.x)
    assertResult(1)(captainMove.y)
    assertResult(1)(captainMove.newX)
    assertResult(2)(captainMove.newY)
    assert(captainMove.isLegal(game))
    captainMove.invoke(game)
    assertResult(Empty())(game.getPiece(1,1))
    assertResult(redCaptain)(game.getPiece(1,2))
    assertResult(1)(captainMove.getActionId)
    assertResult(1)(game.actionList.size)

    json = serializer.writeValueAsString(scoutMove)
    scoutMove = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[MoveAction]
    assertResult(2)(scoutMove.x)
    assertResult(1)(scoutMove.y)
    assertResult(2)(scoutMove.newX)
    assertResult(4)(scoutMove.newY)
    assert(scoutMove.isLegal(game))
    scoutMove.invoke(game)
    assertResult(Empty())(game.getPiece(2,1))
    assertResult(redScout)(game.getPiece(2,4))
    assertResult(2)(scoutMove.getActionId)
    assertResult(2)(game.actionList.size)

    // empty -> anywhere
    var invalidMove = MoveAction(1, 1, 1, 2).withUser(redUser)
    assert(!invalidMove.isLegal(game))
    // non scout with more than two spaces
    invalidMove = MoveAction(1, 2, 1, 4).withUser(redUser)
    assert(!invalidMove.isLegal(game))
    // diagonal
    invalidMove = MoveAction( 1, 2, 2, 3).withUser(redUser)
    assert(!invalidMove.isLegal(game))
  }

  test("CommitAction") {
    var commitActionRed = CommitAction().withUser(redUser.toSimpleUser)
    var commitActionBlue = CommitAction().withUser(blueUser.toSimpleUser)

    var json = serializer.writeValueAsString(commitActionRed)
    commitActionRed = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[CommitAction]
    assertResult(redUser)(commitActionRed.user)

    // need to place all pieces before this is legal
    assert(!commitActionRed.isLegal(game))
    game.redSideboard.foreach(_.clear())
    assert(commitActionRed.isLegal(game))

    commitActionRed.invoke(game)

    assert(game.redPlayerReady)
    assert(game.bluePlayerReady == false)
    assertResult(game.phase)(StrategoPhase.PLACE_PIECES)

    // shouldn't be able to double commit
    assert(!commitActionRed.isLegal(game))

    assert(!commitActionBlue.isLegal(game))
    game.blueSideboard.foreach(_.clear)
    assert(commitActionBlue.isLegal(game))

    commitActionBlue.invoke(game)
    assert(game.bluePlayerReady)
    assertResult(game.phase)(StrategoPhase.RUNNING)


  }

  test("Cheating PlacePieceAction") {
    // should not be able to place a blue piece down on the red side
    var action = PlacePieceAction(1, 1, new BluePiece(MINER_3)).withUser(blueUser.toSimpleUser)
    assert(!action.isLegal(game))
    // should not be able to place a red piece down on the blue side
    action = PlacePieceAction(1, 10, new RedPiece(MINER_3)).withUser(blueUser.toSimpleUser)
    assert(!action.isLegal(game))

    // should not be able to put more than two marshals down
    action = PlacePieceAction(1, 1, new RedPiece(MARSHAL_10)).withUser(redUser.toSimpleUser).asInstanceOf[PlacePieceAction]
    action.invoke(game)
    assert(game.getRedSideboardFor(MARSHAL_10).isEmpty)
    action = PlacePieceAction(1, 2, new RedPiece(MARSHAL_10)).withUser(redUser.toSimpleUser).asInstanceOf[PlacePieceAction]
    assert(!action.isLegal(game))

    // should not be able to place a diff color piece
    action = PlacePieceAction(1, 10, new RedPiece(SCOUT_2)).withUser(blueUser.toSimpleUser)
    assert(!action.isLegal(game))

  }

  test("Cheating attack action") {
    game.setPiece(1,1, new RedPiece(BOMB_11))
    game.setPiece(2,1, new RedPiece(MAJOR_7))
    game.setPiece(1,2, new BluePiece(MAJOR_7))
    // should not be able to attack with a bomb
    var action = AttackAction(1, 1, 1, 2).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))

    // should not be able to attack own piece
    action = AttackAction(2, 1, 1, 1).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))

    // should not be able to attack with diff color
    action = AttackAction(1, 2, 1, 1).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))
  }

  test("Cheating replace action") {
    game.setPiece(1, 1, new RedPiece(MAJOR_7))
    // should not be able to replace another users piece
    var action = ReplacePieceAction(1, 1, 1, 2).withUser(blueUser.toSimpleUser)
    assert(!action.isLegal(game))

    // should not be able to replace your piece to the other side
    action = ReplacePieceAction(1, 1, 1, 10).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))

    // should not be able to replace your piece to a boundary
    action = ReplacePieceAction(1, 1, 2, 5).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(1, 1, 3, 5).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(1, 1, 2, 6).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(1, 1, 3, 6).withUser(redUser.toSimpleUser)
    assert(!action.isLegal(game))
  }

  test ("Cheating move action") {

    game.phase = StrategoPhase.RUNNING
    game.setPiece(1, 1, new RedPiece(MAJOR_7))
    game.setPiece(8, 1, new BluePiece(MAJOR_7))


    var action = MoveAction(1, 1, 1, 2).withUser(redUser)
    assert(action.isLegal(game))
    action.invoke(game)
    assert(!action.isLegal(game))
    action = MoveAction(8, 1, 8, 2).withUser(blueUser)
    assert(action.isLegal(game))
    action.invoke(game)
    assert(!action.isLegal(game))


  }

}
