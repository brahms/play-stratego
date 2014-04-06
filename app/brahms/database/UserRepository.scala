package brahms.database

import org.springframework.data.mongodb.repository.MongoRepository
import brahms.model.User
import org.springframework.stereotype.Repository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.mongodb.core.query.Query._
import org.springframework.data.mongodb.core.query.Criteria._
import java.{lang, util}
import org.springframework.data.mongodb.core.query.Query

trait UserRepository {
  def findByUsername(username: String): Option[User]
  def findAll(): Seq[User]

  def save[S <: User](entites: Iterable[S]): Seq[S]

  def deleteAll(): Unit

  def delete(entities: Iterable[_ <: User]): Unit

  def delete(entity: User): Unit

  def delete(id: String): Unit
  def count(): Long

  def findAll(ids: Iterable[String]): Seq[User]

  def exists(id: String): Boolean

  def findOne(id: String): Option[User]

  def save[S <: User](entity: S): S
}