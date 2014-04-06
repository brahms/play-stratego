package brahms.database.mongo

import brahms.database.UserRepository
import org.springframework.stereotype.Repository
import javax.inject.Inject
import org.springframework.data.mongodb.core.MongoTemplate
import brahms.model.User
import java.{lang, util}
import org.springframework.data.domain.{Sort, Page, Pageable}
import org.springframework.data.mongodb.core.query.Query._
import org.springframework.data.mongodb.core.query.Criteria._
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.query.Query


@Repository
class MongoUserRepository @Inject() (mongoTemplate: MongoTemplate) extends UserRepository{
  val USERS = "users"
  override def findByUsername(username: String): Option[User] = {

    Option(mongoTemplate.findOne(query(where("username").is(username)), classOf[User], USERS ))
  }


  override def findAll(): Seq[User] = {
    mongoTemplate.findAll(classOf[User], USERS).asScala.toList
  }

  override def save[S <: User](entites: Iterable[S]): Seq[S] = {
    entites.map {
      e =>
        mongoTemplate.save(e, USERS)
        e
    }.toList
  }

  override def deleteAll(): Unit = {
    mongoTemplate.remove(new Query, USERS)
  }

  override def delete(entities: Iterable[_ <: User]): Unit = {
    entities.map { e=>
      mongoTemplate.remove(query(where("username").is(e.getUsername)), USERS)
    }
  }

  override def delete(entity: User): Unit = {
    mongoTemplate.remove(query(where("username").is(entity.username)), USERS)
  }

  override def delete(id: String): Unit = {

    mongoTemplate.remove(query(where("_id").is(id)), USERS)
  }

  override def count(): Long = {
    mongoTemplate.count(new Query, USERS)
  }

  override def findAll(ids: Iterable[String]): Seq[User] = {
    mongoTemplate.find(query(where("_id").in(ids)), classOf[User], USERS).asScala
  }

  override def exists(id: String): Boolean = {
    mongoTemplate.count(query(where("_id").is(id)), USERS) > 0
  }

  override def findOne(id: String): Option[User] = {
    Option(mongoTemplate.findById(id, classOf[User], USERS))
  }

  override def save[S <: User](entity: S): S = {
    mongoTemplate.save(entity, USERS)
    entity
  }
}
