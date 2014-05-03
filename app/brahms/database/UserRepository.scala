package brahms.database

import brahms.model.{GameStats, User}
import org.bson.types.ObjectId

trait UserRepository {
  def get(user: User) : User

  def findByUsername(username: String): Option[User]

  def findAll(): Seq[User]

  def save[S <: User](entites: Iterable[S]): Seq[S]

  def deleteAll(): Unit

  def delete(entities: Iterable[_ <: User]): Unit

  def delete(entity: User): Unit

  def delete(username: String): Unit

  def count(): Long

  def findAll(usernames: Iterable[String]): Seq[User]

  def exists(username: String): Boolean

  def save[S <: User](entity: S): S

  def updateUserStats(stats: GameStats): Unit
}