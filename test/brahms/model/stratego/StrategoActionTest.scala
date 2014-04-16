package brahms.model.stratego

import org.scalatest.{BeforeAndAfter, FunSuite}
import brahms.model.stratego.StrategoActions._
import brahms.model.User
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
    redUser.setId(new ObjectId())
    blueUser = new User
    blueUser.setAdmin(false)
    blueUser.setUsername("blueUser")
    blueUser.setId(new ObjectId())
    game = new StrategoGame
    game.setRedPlayer(redUser)
    game.setBluePlayer(blueUser)
    game.init

  }
  test("ReplacePieceAction") {

    val piece = new BluePiece(MAJOR_7)
    val piece2 = new BluePiece(GENERAL_9)
    val placePieceAction = PlacePieceAction(redUser.toSimpleUser, 1, 10 , piece)
    val placePieceAction2 = PlacePieceAction(redUser, 2,10, piece2)
    placePieceAction.invoke(game)
    placePieceAction2.invoke(game)

    assertResult(piece)(game.getPiece(1,10))
    assertResult(piece2)(game.getPiece(2,10))

    var replacePieceAction = ReplacePieceAction(redUser, 1, 10, 2, 10)

    val json = serializer.writeValueAsString(replacePieceAction)

    replacePieceAction = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[ReplacePieceAction]
    assertResult(1)(replacePieceAction.x)
    assertResult(10)(replacePieceAction.y)
    assertResult(2)(replacePieceAction.newX)
    assertResult(10)(replacePieceAction.newY)

    assert(replacePieceAction.isLegal(game))

    replacePieceAction.invoke(game)

    assertResult(Empty)(game.getPiece(1,10))
    assertResult(piece)(game.getPiece(2,10))



  }
  test("PlacePieceAction") {
    val piece = new BluePiece(MAJOR_7)

    val placePieceAction = PlacePieceAction(redUser.toSimpleUser, 1, 10 , piece)
    val json = serializer.writeValueAsString(placePieceAction)
    val actualAction = serializer.readValue(json, classOf[StrategoAction])
    assert(actualAction.isInstanceOf[PlacePieceAction])
    val actualPlaceAction = actualAction.asInstanceOf[PlacePieceAction]
    assertResult(placePieceAction.user)(actualPlaceAction.user)
    assertResult(placePieceAction.x)(actualPlaceAction.x)
    assertResult(placePieceAction.y)(actualPlaceAction.y)
    assertResult(UNKNOWN_13)(actualPlaceAction.mask(blueUser).piece.value)

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
    game.setStrategoState(StrategoState.RUNNING)
    val attackActionToSerialize = AttackAction(redUser.toSimpleUser, 1, 1, 1, 2)
    val json = serializer.writeValueAsString(attackActionToSerialize)
    println(json)
    val attackAction = serializer.readValue(json, classOf[StrategoAction])
    val redPiece = new RedPiece(LIEUTENANT_5)
    game.setPiece(1,1, redPiece)
    game.setPiece(1,2, new BluePiece(SPY_1))
    assert(attackAction.isLegal(game))
    attackAction.invoke(game)
    assertResult (Empty) ( game.getPiece(1, 1) )
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
    game.setStrategoState(StrategoState.RUNNING)

    var captainMove = MoveAction(redUser, 1, 1, 1, 2)
    var scoutMove = MoveAction(redUser, 2, 1, 2, 4)

    var json = serializer.writeValueAsString(captainMove)
    captainMove = serializer.readValue(json, classOf[StrategoAction]).asInstanceOf[MoveAction]
    assertResult(1)(captainMove.x)
    assertResult(1)(captainMove.y)
    assertResult(1)(captainMove.newX)
    assertResult(2)(captainMove.newY)
    assert(captainMove.isLegal(game))
    captainMove.invoke(game)
    assertResult(Empty)(game.getPiece(1,1))
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
    assertResult(Empty)(game.getPiece(2,1))
    assertResult(redScout)(game.getPiece(2,4))
    assertResult(2)(scoutMove.getActionId)
    assertResult(2)(game.actionList.size)

    // empty -> anywhere
    var invalidMove = MoveAction(redUser, 1, 1, 1, 2)
    assert(!invalidMove.isLegal(game))
    // non scout with more than two spaces
    invalidMove = MoveAction(redUser, 1, 2, 1, 4)
    assert(!invalidMove.isLegal(game))
    // diagonal
    invalidMove = MoveAction(redUser, 1, 2, 2, 3)
    assert(!invalidMove.isLegal(game))
  }

  test("CommitAction") {
    var commitActionRed = CommitAction(redUser)
    var commitActionBlue = CommitAction(blueUser)

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
    assertResult(game.strategoState)(StrategoState.PLACE_PIECES)

    // shouldn't be able to double commit
    assert(!commitActionRed.isLegal(game))

    assert(!commitActionBlue.isLegal(game))
    game.blueSideboard.foreach(_.clear)
    assert(commitActionBlue.isLegal(game))

    commitActionBlue.invoke(game)
    assert(game.bluePlayerReady)
    assertResult(game.strategoState)(StrategoState.RUNNING)


  }

  test("Cheating PlacePieceAction") {
    // should not be able to place a blue piece down on the red side
    var action = PlacePieceAction(blueUser, 1, 1, new BluePiece(MINER_3))
    assert(!action.isLegal(game))
    // should not be able to place a red piece down on the blue side
    action = PlacePieceAction(blueUser, 1, 10, new RedPiece(MINER_3))
    assert(!action.isLegal(game))

    // should not be able to put more than two marshals down
    action = PlacePieceAction(redUser, 1, 1, new RedPiece(MARSHAL_10))
    action.invoke(game)
    assert(game.getRedSideboardFor(MARSHAL_10).isEmpty)
    action = PlacePieceAction(redUser, 1, 2, new RedPiece(MARSHAL_10))
    assert(!action.isLegal(game))

    // should not be able to place a diff color piece
    action = PlacePieceAction(blueUser, 1, 10, new RedPiece(SCOUT_2))
    assert(!action.isLegal(game))

  }

  test("Cheating attack action") {
    game.setPiece(1,1, new RedPiece(BOMB_11))
    game.setPiece(2,1, new RedPiece(MAJOR_7))
    game.setPiece(1,2, new BluePiece(MAJOR_7))
    // should not be able to attack with a bomb
    var action = AttackAction(redUser, 1, 1, 1, 2)
    assert(!action.isLegal(game))

    // should not be able to attack own piece
    action = AttackAction(redUser, 2, 1, 1, 1)
    assert(!action.isLegal(game))

    // should not be able to attack with diff color
    action = AttackAction(redUser, 1, 2, 1, 1)
    assert(!action.isLegal(game))
  }

  test("Cheating replace action") {
    game.setPiece(1, 1, new RedPiece(MAJOR_7))
    // should not be able to replace another users piece
    var action = ReplacePieceAction(blueUser, 1, 1, 1, 2)
    assert(!action.isLegal(game))

    // should not be able to replace your piece to the other side
    action = ReplacePieceAction(redUser, 1, 1, 1, 10)
    assert(!action.isLegal(game))

    // should not be able to replace your piece to a boundary
    action = ReplacePieceAction(redUser, 1, 1, 2, 5)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(redUser, 1, 1, 3, 5)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(redUser, 1, 1, 2, 6)
    assert(!action.isLegal(game))
    action = ReplacePieceAction(redUser, 1, 1, 3, 6)
    assert(!action.isLegal(game))
  }

}
