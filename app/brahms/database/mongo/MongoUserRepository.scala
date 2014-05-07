package brahms.database.mongo

import brahms.database.UserRepository
import org.springframework.stereotype.Repository
import brahms.model.{GameStats, User}
import scala.collection.JavaConverters._
import org.bson.types.ObjectId
import javax.inject.Inject
import org.jongo.Jongo


@Repository
class MongoUserRepository @Inject() (jongo: Jongo) extends AbstractMongoRepository(jongo) with UserRepository {
  val USERS = "users"
  val users = jongo.getCollection(USERS)
  users.ensureIndex( "{ username: 1 }", "{ unique: true, dropDups: true}" )


  override def get(user: User): User = {
    logger.debug("get: " + user.getUsername)
    findByUsername(user.username).get
  }

  override def findByUsername(username: String): Option[User] = {
    logger.debug("Finding: {}", username)
    Option(users.findOne("{username:#}", username).as(classOf[User]))
  }


  override def findAll(): Seq[User] = {
    users.find().as(classOf[User]).asScala.toSeq
  }

  override def save[S <: User](entites: Iterable[S]): Seq[S] = {
    logger.trace("Save multiple")
    entites.map {
      e =>
        logger.debug("Saving: {}", e)
        save(e)
        logger.debug("Saved: {}", e)
        e
    }.toSeq
  }

  override def deleteAll(): Unit = {
    logger.trace("deleteAll")
    users.remove()
    assert(users.count() == 0)
  }

  override def delete(entities: Iterable[_ <: User]): Unit = {
    entities.foreach {
      e =>
        delete(e)
    }
  }

  override def delete(entity: User): Unit = {
    delete(entity.username)
  }

  override def delete(username: String): Unit = {
    logger.debug("Deleting user: {}", username)
    users.remove("{username:#}", username)
  }

  override def count(): Long = {
    users.count()
  }

  override def findAll(usernames: Iterable[String]): Seq[User] = {
    users.find("{username: {$in : #} }", usernames).as(classOf[User]).asScala.toSeq
  }

  override def exists(username: String): Boolean = {
    users.count("{username: #}", username) == 1
  }


  override def save[S <: User](entity: S): S = {
    logger.debug("Saving {}", entity.toJson)
    if (entity.id == null) entity.id = new ObjectId().toString
    assert(entity.username != null)
    users.save(entity)
    logger.debug("Saved")
    entity
  }

  override def updateUserStats(stats: GameStats): Unit = {
    val users = findAll(stats.players.map(_.getUsername))
    logger.debug(s"Updating User Stats users: $users with stats $stats")
    users.foreach {
      user =>
        user.playedGames :+= stats.game.getId.toString
        user.currentGameId = None
    }

    stats.winners.foreach {
      winner =>
        users.find(_.equals(winner)).get.wonGames :+= stats.game.getId.toString
    }

    stats.losers.foreach {
      loser =>
        users.find(_.equals(loser)).get.lostGames :+= stats.game.getId.toString
    }

    stats.draws.foreach {
      draw =>
        users.find(_.equals(draw)).get.drawnGames :+= stats.game.getId.toString
    }

    logger.debug("Saving {} users", users.size)
    save(users)
  }
}
