package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoTypes._
import brahms.model.{GameState, User}
import brahms.serializer.{JsonViews, Serializer}
import brahms.model.stratego.StrategoActions.{CommitAction, PlacePieceAction, MoveAction}
import org.bson.types.ObjectId


class StrategoGameTest extends FunSuite {

  val serializer = Serializer.serializer.writerWithView(JsonViews.PUBLIC)
  test("Create a stratego game") {
    val board = new StrategoGame
    val red = new User
    red.setUsername("red")
    red.setId(new ObjectId())

    val blue = new User
    blue.setUsername("blue")
    blue.setId(new ObjectId())

    board.setRedPlayer(red)
    board.setBluePlayer(blue)
    board.init


    board.setPiece(1,1, new BluePiece(1))
    board.setPiece(2,2, new BluePiece(1))
    board.strategoState = StrategoState.RUNNING

    val action =  MoveAction(blue,1,1,1,2)
    assert(action.isLegal(board))
    action.invoke(board)
    assert(board.board(1)(2) != Empty)
    assert(board.currentPlayer == board.bluePlayer)
    board.actionList += action

    println(Serializer.serializer.writeValueAsString(board.mask(red)))

  }

  test("boundaryInPath") {
    val game = new StrategoGame
    game.setRedPlayer(new User)
    game.setBluePlayer(new User)
    game.init
    assert(game.boundaryInPath(3,1,3,10))
    assert(!game.boundaryInPath(1,1,1,10))
    assert(game.boundaryInPath(1,5,10,5))

    // lets go reverse
    assert(game.boundaryInPath(3,10, 3, 1))
    assert(!game.boundaryInPath(1,10, 1, 1))
    assert(game.boundaryInPath(10,5, 1, 5))
  }

  test("Test some serialization") {
    val game = new StrategoGame
    val redPlayer = new User
    val bluePlayer = new User
    redPlayer.setUsername("cbrahms")
    bluePlayer.setUsername("bla")
    game.setState(GameState.RUNNING)
    game.setRedPlayer(redPlayer)
    game.setBluePlayer(bluePlayer)
    game.init
    println (Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(game.mask(redPlayer)))
  }

  test("Test show serialization game") {
    val game = new StrategoGame
    val red = new User
    val blue = new User
    red.setUsername("RedPlayer")
    blue.setUsername("BluePlayer")
    red.setId(new ObjectId())
    blue.setId(new ObjectId())
    game.setRedPlayer(red)
    game.setBluePlayer(blue)
    game.init
    var value = 1
    (1 to 10).foreach {
      x =>
        (1 to 4).foreach {
          y =>
            if (!game.stillInSideboard(new RedPiece(value))) value += 1
            val bluePiece = new BluePiece(value)
            val redPiece = new RedPiece(value)

            var action = PlacePieceAction(red, x, y, redPiece)
            assert(action.isLegal(game))
            action.invoke(game)
            action = PlacePieceAction(blue, x, 11-y, bluePiece)
            assert(action.isLegal(game))
            action.invoke(game)

        }
    }
    CommitAction(red).invoke(game)
    CommitAction(blue).invoke(game)
    MoveAction(red, 1, 4, 1, 5).invoke(game)
    println(serializer.writeValueAsString(game.mask(red)))


  }


}
