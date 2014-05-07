package brahms.database.mongo

import brahms.database.GameRepository
import brahms.model.{GameState, Game}

import scala.collection.JavaConverters._

import org.springframework.stereotype.Repository
import org.bson.types.ObjectId
import javax.inject.Inject
import org.jongo.Jongo
import org.jongo.Oid.withOid;

@Repository
class MongoGameRepository @Inject()(jongo: Jongo) extends AbstractMongoRepository(jongo) with GameRepository {
  val GAMES = "games";
  val games = jongo.getCollection(GAMES)
  override def findOne(id: String): Option[Game] = {
    Option(games.findOne(withOid(id)).as(classOf[Game]))
  }

  override def findAll(ids: Iterable[String]): Seq[Game] = {
    games.find("{id: {$in:#}}", ids.asJava).as(classOf[Game]).asScala.toSeq
  }

  override def deleteAll(): Unit = {
    logger.debug("deleteAll")
    games.remove()
    assert(games.count()==0)
  }

  override def delete(entities: Iterable[_ <: Game]): Unit = {
    games.remove("{id: {$in:#}}", entities.map(_.getId))
  }

  override def delete(id: String): Unit = {
    games.remove(id)
  }

  override def count(): Long = {
    games.count()
  }

  override def exists(id: String): Boolean = {
    val count = games.count("{id: #}", id)
    logger.debug("Got count: {} for id: {}", count, id)
    count > 0
  }

  override def save[S <: Game](entity: S): S = {
    logger.debug("Saving game")

    if (entity.id == null) entity.id = new ObjectId().toString
    games.save(entity)
    logger.debug("Game saved: {}", entity)
    entity
  }

  override def delete(entity: Game): Unit = {
    if (entity.getId != null) {
      logger.debug("Deleting game: {}", entity.getId)
      games.remove(entity.getId)
    }
  }

  override def findPending: Seq[Game] = {
    games.find("{state: #}", GameState.PENDING.toString).as(classOf[Game]).asScala.toSeq
  }

  override def findOnePending(id: String): Option[Game] = {
    findOne(id) match {
      case Some(game) if (game.state == GameState.PENDING) =>
        Some(game)
      case _ => None
    }
  }

  override def findOneRunning(id: String): Option[Game] = {
    findOne(id) match {
      case Some(game) if (game.state == GameState.RUNNING) =>
        Some(game)
      case _ => None
    }
  }

  override def findRunning: Seq[Game] = {
    logger.debug("findRunning")
    val seq = games.find("{state: #}", GameState.RUNNING.toString).as(classOf[Game]).asScala.toSeq
    logger.debug("Found: {}", seq)
    seq
  }

  override def save[S <: Game](entities: Iterable[S]): Unit = {
    logger.debug("saving {} games", entities.size)
    entities.foreach { e =>
      save(e)
    }
  }

  override def findAll(): Seq[Game] = {
    games.find().as(classOf[Game]).asScala.toSeq
  }
}
