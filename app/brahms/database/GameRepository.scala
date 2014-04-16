package brahms.database

import brahms.model.Game
import org.bson.types.ObjectId

trait GameRepository {
  def findOne(id: ObjectId): Option[Game]
  def findAll(ids: Iterable[String]): Seq[Game]
  def findPending: Seq[Game]
  def deleteAll(): Unit
  def delete(entities: Iterable[_ <: Game]): Unit
  def delete(id: ObjectId): Unit
  def delete(entity: Game): Unit
  def count(): Long
  def exists(id: ObjectId) : Boolean
  def save[S <: Game](entity: S): S
}
