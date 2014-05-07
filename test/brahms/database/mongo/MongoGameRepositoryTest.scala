package brahms.database.mongo

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{TestContextManager, ContextConfiguration}
import javax.inject.Inject
import brahms.database.{UserRepository, GameRepository}
import brahms.model.stratego.{StrategoGame}
import brahms.model.User
import brahms.test.TestSpringConfig
import brahms.model.stratego.StrategoActions.PlacePieceAction
import brahms.model.stratego.StrategoTypes._
import brahms.serializer.{JsonViews, Serializer}

@ContextConfiguration(
  classes = Array(classOf[TestSpringConfig]),
  loader = classOf[AnnotationConfigContextLoader])
class MongoGameRepositoryTest extends FunSuite with BeforeAndAfter {
  @Inject
  var gameRepo: GameRepository = _
  @Inject
  var userRepo: UserRepository = _

  var game = new StrategoGame
  val redPlayer: User = {
    val user = new User
    user.setUsername("redPlayer")
    user
  }
  val bluePlayer: User =  {
    val user = new User
    user.setUsername("bluePlayer")
    user
  }

  new TestContextManager(this.getClass).prepareTestInstance(this)
  before {
    userRepo.save(redPlayer)
    userRepo.save(bluePlayer)
    game.setRedPlayer(redPlayer)
    gameRepo.save(game)
    assert(Option(game.id).isDefined)
  }

  after {
    userRepo.delete(List(redPlayer, bluePlayer))
    gameRepo.delete(game)
  }

  test ("Create a game") {
    assert(gameRepo.exists(game.id))
  }

  test ("Find a game") {
    val retrieved = gameRepo.findOne(game.id).get.asInstanceOf[StrategoGame]
    assertResult(redPlayer)(retrieved.redPlayer)
    assertResult(game.strategoState)(retrieved.strategoState)
  }

  test ("Update a game") {
    game.setBluePlayer(bluePlayer)
    game.init
    val action = PlacePieceAction(1,10, new BluePiece(MAJOR_7)).withUser(bluePlayer)
    assert(action.isLegal(game))
    action.invoke(game)
    gameRepo.save(game)

    val retrieved = gameRepo.findOne(game.id).get.asInstanceOf[StrategoGame]
    assertResult(redPlayer)(retrieved.redPlayer)
    assertResult(game.strategoState)(retrieved.strategoState)
    assert(retrieved.actionList.isEmpty==false)
    assertResult(classOf[PlacePieceAction])(retrieved.actionList(0).getClass)
    assertResult(1)(retrieved.actionList(0).getActionId)

    println(Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(retrieved))


  }

}
