package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoType.{Empty, BluePiece}
import brahms.model.User
import brahms.serializer.Serializer
import brahms.model.stratego.StrategoActions.MoveAction
import brahms.model.stratego.StrategoGame.StrategoState


class StrategoGameTest extends FunSuite {

  test("Create a stratego game") {
    val board = new StrategoGame
    board.init
    val red = new User
    red.setUsername("red")
    red.setId("sd")

    val blue = new User
    blue.setUsername("blue")
    blue.setId("sd")


    board.setPiece(1,1, new BluePiece(1))
    board.setPiece(2,2, new BluePiece(1))
    board.setRedPlayer(red)
    board.setCurrentPlayer(board.getRedPlayer)
    board.setBluePlayer(blue)
    board.strategoState = StrategoState.RUNNING

    val action =  MoveAction(blue,1,1,1,2)
    assert(action.isLegal(board))
    action.invoke(board)
    assert(board.board(1)(2) != Empty)
    assert(board.currentPlayer == board.bluePlayer)
    board.actionList += action

    println(board.toString)

    println(Serializer.serializer.writeValueAsString(board))

  }


}
