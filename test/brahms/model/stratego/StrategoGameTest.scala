package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.stratego.StrategoTypes._
import brahms.model.{Game, GameState, User}
import brahms.serializer.{JsonViews, Serializer}
import brahms.model.stratego.StrategoActions.{CommitAction, PlacePieceAction, MoveAction}
import org.bson.types.ObjectId


class StrategoGameTest extends FunSuite {

  val serializer = Serializer.serializer.writerWithView(JsonViews.PUBLIC)
  test("Create a stratego game") {
    val board = new StrategoGame
    val red = new User
    red.setUsername("red")

    val blue = new User
    blue.setUsername("blue")
    board.setRedPlayer(red)
    board.setBluePlayer(blue)
    board.init


    board.setPiece(1,1, new BluePiece(1))
    board.setPiece(2,2, new BluePiece(1))
    board.phase = StrategoPhase.RUNNING

    val action =  MoveAction(1,1,1,2).withUser(blue).asInstanceOf[StrategoAction]
    assert(action.isLegal(board))
    action.invoke(board)
    assert(board.board(1)(2) != Empty)
    assert(board.currentPlayer == board.bluePlayer)

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
    game.setRedPlayer(redPlayer.toSimpleUser)
    game.setBluePlayer(bluePlayer.toSimpleUser)
    game.init
    println (Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(game.mask(redPlayer)))
  }

  test("Test show serialization game") {
    val game = new StrategoGame
    val red = new User
    val blue = new User
    red.setUsername("RedPlayer")
    blue.setUsername("BluePlayer")

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

            var action = PlacePieceAction(x, y, redPiece).withUser(red)
            assert(action.isLegal(game))
            action.invoke(game)
            action = PlacePieceAction(x, 11-y, bluePiece).withUser(blue)
            assert(action.isLegal(game))
            action.invoke(game)

        }
    }
    CommitAction().invoke(game)
    CommitAction().invoke(game)
    MoveAction(1, 4, 1, 5).withUser(red).invoke(game)
    println(serializer.writeValueAsString(game.mask(red)))

    val json = Serializer.serializer.writeValueAsString(game)
    val deser = Serializer.serializer.readValue(json, classOf[Game])

  }

  test("deser") {
    val json = """{
                 |    "type": "Stratego",
                 |    "redPlayer": {
                 |        "username": "local1",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "536874f9b968ce471508a318"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    },
                 |    "bluePlayer": {
                 |        "username": "local2",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "5368751db968ce471508a31a"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    },
                 |    "currentPlayer": {
                 |        "username": "local1",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "536874f9b968ce471508a318"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    },
                 |    "bluePlayerReady": false,
                 |    "redPlayerReady": false,
                 |    "state": "RUNNING",
                 |    "creator": {
                 |        "username": "local1",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "536874f9b968ce471508a318"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    },
                 |    "actionList": [{
                 |        "x": 1,
                 |        "y": 1,
                 |        "piece": {
                 |            "type": "RedPiece",
                 |            "value": 1
                 |        },
                 |        "actionId": 1,
                 |        "user": {
                 |            "username": "local1",
                 |            "admin": false,
                 |            "_id": {
                 |                "$oid": "536874f9b968ce471508a318"
                 |            },
                 |            "simple": true,
                 |            "wonGames": [],
                 |            "lostGames": [],
                 |            "drawnGames": [],
                 |            "playedGames": []
                 |        }
                 |    }],
                 |    "timeouts": {
                 |        "local1": 1399354684535
                 |    },
                 |    "players": [{
                 |        "username": "local1",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "536874f9b968ce471508a318"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    }, {
                 |        "username": "local2",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "5368751db968ce471508a31a"
                 |        },
                 |        "password": "$2a$10$AlyaX3boONXrddMStknKP.UhbDSfl7fKJUxpSLXD.w8sI4StwQ3nS",
                 |        "currentGameId": {
                 |            "$oid": "53687500b968ce471508a319"
                 |        },
                 |        "simple": false,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": [],
                 |        "stats": {
                 |            "won": 0,
                 |            "lost": 0,
                 |            "drawn": 0,
                 |            "played": 0
                 |        }
                 |    }],
                 |    "gameOver": false,
                 |    "board": [
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 1
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Empty"
                 |        }, {
                 |            "type": "Boundary"
                 |        }],
                 |        [{
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }, {
                 |            "type": "Boundary"
                 |        }]
                 |    ],
                 |    "blueSideboard": [
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 1
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 2
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 3
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 4
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 5
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 6
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 7
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 7
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 7
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 8
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 8
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 9
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 10
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "BluePiece",
                 |            "value": 11
                 |        }],
                 |        [{
                 |            "type": "BluePiece",
                 |            "value": 12
                 |        }]
                 |    ],
                 |    "redSideboard": [
                 |        [],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 2
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 3
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 3
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 4
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 4
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 5
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 5
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 6
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 6
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 7
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 7
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 7
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 8
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 8
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 9
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 10
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }, {
                 |            "type": "RedPiece",
                 |            "value": 11
                 |        }],
                 |        [{
                 |            "type": "RedPiece",
                 |            "value": 12
                 |        }]
                 |    ],
                 |    "_id": {
                 |        "$oid": "53687500b968ce471508a319"
                 |    },
                 |    "phase": "PLACE_PIECES"
                 |}]
                 |""".stripMargin
    val game = Serializer.serializer.readValue(json, classOf[Game])
    assert(true)
  }


}
