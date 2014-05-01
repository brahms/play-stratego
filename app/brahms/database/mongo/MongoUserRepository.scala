package brahms.database.mongo

import brahms.database.UserRepository
import org.springframework.stereotype.Repository
import brahms.model.User
import scala.collection.JavaConverters._
import org.bson.types.ObjectId
import javax.inject.Inject
import org.jongo.Jongo


@Repository
class MongoUserRepository @Inject() (jongo: Jongo) extends AbstractMongoRepository(jongo) with UserRepository {
  val USERS = "users"
  val users = jongo.getCollection(USERS)

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
        users.save(e)
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
    delete(entity.id)
  }

  override def delete(id: ObjectId): Unit = {
    logger.debug("Deleting user: {}", id)
    users.remove(id)
  }

  override def count(): Long = {
    users.count()
  }

  override def findAll(ids: Iterable[String]): Seq[User] = {
    users.find().as(classOf[User]).asScala.toSeq
  }

  override def exists(id: ObjectId): Boolean = {
    users.count("{_id: #}", id) == 1
  }

  override def findOne(id: ObjectId): Option[User] = {
    Option(users.findOne(id).as(classOf[User]))
  }

  override def save[S <: User](entity: S): S = {
    users.save(entity)
    entity
  }
}
