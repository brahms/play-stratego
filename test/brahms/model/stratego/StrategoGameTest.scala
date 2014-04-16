package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoTypes._
import brahms.model.User
import brahms.serializer.Serializer
import brahms.model.stratego.StrategoActions.MoveAction
import org.bson.types.ObjectId


class StrategoGameTest extends FunSuite {

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


}
