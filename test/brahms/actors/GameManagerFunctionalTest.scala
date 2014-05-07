package brahms.actors


import org.springframework.scala.context.function._
import org.specs2.mutable._
import brahms.database.{UserRepository, GameRepository}
import com.mongodb.{MongoClientURI, MongoClient}
import org.jongo.Jongo
import brahms.serializer.Serializer
import brahms.database.mongo.{MongoUserRepository, MongoGameRepository}
import brahms.model.{GameState, User}
import org.bson.types.ObjectId
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import brahms.model.stratego.{StrategoTypes, StrategoPhase, StrategoGame}
import brahms.actors._
import brahms.model.stratego.StrategoActions.PlacePieceAction
import brahms.model.stratego.StrategoTypes.{BluePiece, RedPiece}
import brahms.util.WithLogging

class TestAppConfiguration extends FunctionalConfiguration {
  importClass(
    classOf[GameManager],
    classOf[ActorSystemBean],
    classOf[MongoGameRepository],
    classOf[MongoUserRepository])

  val mongoClient = bean() {
    val mongoClientUri = new MongoClientURI("mongodb://cbrahms:OneOne11@localhost/test")
    new MongoClient(mongoClientUri)
  }
  val jongo = bean() {
    new Jongo(mongoClient().getDB("test"), Serializer.createJongoMapper)
  }
}

/**
 * Tests the creating of a game, and playing it from an empty test db
 */
class GameManagerFunctionalTest extends Specification with WithLogging {

  val serializer = Serializer.serializer
  val PLAYER1 = new User
  PLAYER1.setAdmin(false)
  PLAYER1.setUsername("player1")

  implicit val timeout = Timeout((5 seconds).inMillis)
  implicit val timeoutDuration = timeout.duration
  val PLAYER2 = new User
  PLAYER2.setUsername("player2")
  PLAYER2.setAdmin(false)
  var gameId: String = null


  "GameManager" should {

    sequential

    implicit val ctx = FunctionalConfigApplicationContext(classOf[TestAppConfiguration])

    val userRepo = ctx.getBean(classOf[UserRepository])
    val gameRepo = ctx.getBean(classOf[GameRepository])
    userRepo.deleteAll
    gameRepo.deleteAll
    userRepo.save(List(PLAYER1, PLAYER2))

    val actorSystem = ctx.getBean(classOf[ActorSystemBean])
    val gameManager = actorSystem.gameManager

    "Create both users" in {
      userRepo.findByUsername("player1") must beLike {case Some(user) => ok}
      userRepo.findByUsername("player2") must beLike {case Some(user) => ok}
    }
    "Player1 should be able to create a game" in  {
        val player = userRepo.findByUsername("player1").get
        val result = gameManager ? CreateGame(player, "stratego") map {
          case success: CreateGameSucceeded =>
            gameId = success.game.id
            (success.game should beAnInstanceOf[StrategoGame])
          case result =>
            ko("Invalid response: " + result)
        }
        Await.result(result, timeoutDuration)
      }
    "Player1 should not be able to create another game" in {
      val player = userRepo.findByUsername("player1").get
      player.getCurrentGameId must not(beNone)
      logger.info(serializer.writeValueAsString(player))
      val result = gameManager ? CreateGame(player, "stratego") map {
        case failure: Failed => ok
        case _ => ko("Should fail")
      }
      Await.result(result, timeoutDuration)
    }
    "Player1 should not be able to join his own game (he already is in it)" in {
      Await.result((gameManager ? JoinGame(PLAYER1, gameId.toString)).map(_ should beLike {case _:Failed => ok}), timeoutDuration)
    }
    "Player 1 should not be able to invoke an action while pending" in {
      val result = gameManager ? InvokeActionRequest(PLAYER1, "") map {
        case InvokeActionSucceeded => ko("Should not of invoked")
        case Failed(reason) => ok
      }
      Await.result(result, timeoutDuration)
    }
    "Player 2 should now be able to join this game" in {
      val games = gameRepo.findPending
      games.length should beEqualTo(1)
      val game = games(0)
      game should beAnInstanceOf[StrategoGame]

      val result = gameManager ? JoinGame(PLAYER2, game.getId.toString) map {
        case Failed(reason) => ko("Failed to join game: " + reason)
        case JoinGameSucceeded(game: StrategoGame) =>
          gameId = game.id
          (game.state should beEqualTo(GameState.RUNNING)) and (game.phase should beEqualTo(StrategoPhase.PLACE_PIECES))
        case _ => ko
      }
      Await.result(result, timeoutDuration)
    }
    "Player 1 should be in the game when retrieved from the db" in {
      val player1 = userRepo.findByUsername("player1").get
      player1.getCurrentGameId should beEqualTo(Some(gameId))
    }
    "Player 2 should be in the game when retrieved from the db" in {
      val player2 = userRepo.findByUsername("player2").get
      player2.getCurrentGameId should beEqualTo(Some(gameId))
    }
    "The game should be in the db" in {
      val game = gameRepo.findOne(gameId).map(_.asInstanceOf[StrategoGame])
      game should not(beNone)
      game.get.redPlayer should beEqualTo(PLAYER1)
      game.get.currentPlayer should beEqualTo(PLAYER1)
      game.get.bluePlayer should beEqualTo(PLAYER2)
      game.get.state should beEqualTo(GameState.RUNNING)
      game.get.phase should beEqualTo(StrategoPhase.PLACE_PIECES)
    }
    "Player 1 should be able to place a piece" in {
      val json = serializer.writeValueAsString(PlacePieceAction(1, 1, new RedPiece(StrategoTypes.SCOUT_2)).withUser(PLAYER1))
      val player1 = userRepo.findByUsername("player1").get
      val result = gameManager ? InvokeActionRequest(player1, json) map {
        case Failed(reason) => ko("Failed to invoke action: " + reason)
        case InvokeActionSucceeded => ok
        case _ => ko
      }
      Await.result(result, timeoutDuration)
    }
    "Player 2 should be able to place several pieces in consecutive requests before player 1 for the place pieces phase" in {
      var json = serializer.writeValueAsString(PlacePieceAction(1, 9, new BluePiece(StrategoTypes.SCOUT_2)).withUser(PLAYER2))
      val player2 = userRepo.findByUsername("player2").get

      val result1 = Await.result((gameManager ? InvokeActionRequest(player2, json)).map(_ should beLike {case InvokeActionSucceeded => ok}), timeoutDuration)
      json = serializer.writeValueAsString(PlacePieceAction(1, 8, new BluePiece(StrategoTypes.SCOUT_2)).withUser(PLAYER2))
      val result2 = Await.result((gameManager ? InvokeActionRequest(player2, json)).map(_ should beLike {case InvokeActionSucceeded => ok}), timeoutDuration)
      json = serializer.writeValueAsString(PlacePieceAction( 1, 7, new BluePiece(StrategoTypes.SCOUT_2)).withUser(PLAYER2))
      val result3 = Await.result((gameManager ? InvokeActionRequest(player2, json)).map(_ should beLike {case InvokeActionSucceeded => ok}), timeoutDuration)

      result1 and result2 and result3
    }
    "Shut down the ctx" in {
      ctx.close()
      ok
    }
  }


}
