package brahms.cache
import java.util.UUID
import play.api.cache.Cache
import brahms.model.User
import scala.beans.BeanProperty
import brahms.serializer.Serializer
import scala.concurrent.duration._
import play.api.Application

class NoSessionError(message: String = "No Session") extends Exception(message)

class ExpiredSessionError(message: String = "Expired Session") extends Exception(message)

class ServerSideSessionState {
  var user: Option[User] = None
}
object ServerSideSessions {

  def get(token: String)(implicit app: Application): Option[ServerSideSessionState] = {

    Cache.getAs[String](token).map {
      json =>
      //  println(s"XXX get server session $token = $json")
        val state = Serializer.serializer.readValue(json, classOf[ServerSideSessionState])
      //  println(s"XXX get server session $token = $state")
        state
    }
  }

  def delete(token: String)(implicit app: Application) {
    Cache.remove(token)
  }

  /**
   * Generate a new unique session key.
   *
   * @return a new session key
   */
  def newToken: String = UUID.randomUUID().toString

  /**
   * Create a new session and save it.
   *
   * @return the key of the new session.
   */
  def create(state: ServerSideSessionState, expiry: Duration = (24 hours))(implicit app: Application): String = {
    val token = newToken
    update(token, state, expiry)
    token
  }

  def update(token: String, state: ServerSideSessionState, expiry: Duration = (24 hours))(implicit app: Application): ServerSideSessionState = {
    //println(s"XXX update token=$token state=$state json=${JsonMapper.toJson(state).toString}")
    val json = Serializer.serializer.writeValueAsString(state)
    Cache.set(token, json, expiry)
    //println("IMMEDIATE GET="+Cache.get[ServerSideSessionState](token))
    state
  }
}