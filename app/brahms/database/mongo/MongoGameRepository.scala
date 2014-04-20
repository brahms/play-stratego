package brahms.database.mongo

import brahms.database.GameRepository
import brahms.model.{GameState, Game}

import scala.collection.JavaConverters._

import org.springframework.stereotype.Repository
import org.bson.types.ObjectId
import javax.inject.Inject
import org.jongo.Jongo


@Repository
class MongoGameRepository @Inject()(jongo: Jongo) extends AbstractMongoRepository(jongo) with GameRepository {
  val GAMES = "games";
  val games = jongo.getCollection(GAMES)
  override def findOne(id: ObjectId): Option[Game] = {
    Option(games.findOne(id).as(classOf[Game]))
  }

  override def findAll(ids: Iterable[String]): Seq[Game] = {
    games.find("{_id: {$in:#}}", ids.asJava).as(classOf[Game]).asScala.toSeq
  }

  override def deleteAll(): Unit = {
    games.remove()
  }

  override def delete(entities: Iterable[_ <: Game]): Unit = {
    games.remove("{_id: {$in:#}}", entities.map(_.getId))
  }

  override def delete(id: ObjectId): Unit = {
    games.remove(id)
  }

  override def count(): Long = {
    games.count()
  }

  override def exists(id: ObjectId): Boolean = {
    val count = games.count("{_id: #}", id)
    logger.debug("Got count: {} for id: {}", count, id)
    count > 0
  }

  override def save[S <: Game](entity: S): S = {
    logger.debug("Saving game")
    games.save(entity)
    logger.debug("Game saved: {}", entity.getId)
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
}
