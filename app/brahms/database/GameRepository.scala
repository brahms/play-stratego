package brahms.database

import brahms.model.Game

trait GameRepository {
  def findOne(id: String): Option[Game]
  def findOnePending(id: String): Option[Game]
  def findOneRunning(id: String): Option[Game]
  def findAll(ids: Iterable[String]): Seq[Game]
  def findPending: Seq[Game]
  def deleteAll(): Unit
  def delete(entities: Iterable[_ <: Game]): Unit
  def delete(id: String): Unit
  def delete(entity: Game): Unit
  def count(): Long
  def exists(id: String) : Boolean
  def save[S <: Game](entity: S): S
  def save[S <: Game](entities: Iterable[S]): Unit
  def findRunning: Seq[Game];
  def findAll(): Seq[Game]
}
