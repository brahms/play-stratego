package brahms.model
import scala.beans.{BeanProperty, BooleanBeanProperty}
import org.springframework.security.crypto.bcrypt.BCrypt
import brahms.util.WithLogging
import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import brahms.serializer.{Serializer, JsonViews}
import org.jongo.marshall.jackson.oid.Id
import org.bson.types.ObjectId

object User extends WithLogging{
  val USERNAME_PATTERN = "[a-zA-Z][a-zA-Z0-9]{3,10}"
  val PASSWORD_MIN = 6
  val PASSWORD_MAX = 100
  def encryptPassword(password: String): String = {
    val salt = BCrypt.gensalt(10)
    val enc = BCrypt.hashpw(password, salt)
    logger.debug(s"converted $password to $enc")
    enc
  }

  def validateUsername(username: String) : Boolean = {
    username.matches(USERNAME_PATTERN)
  }

  def validatePassword(password: String) : Boolean = {
    if (password.size >= PASSWORD_MIN && password.size < PASSWORD_MAX)
      true
    else
      false
  }
}


class User {

  @Id
  @BeanProperty
  var id: ObjectId = _;

  @BeanProperty
  var username: String = _;

  @BeanProperty
  @JsonView(Array(classOf[JsonViews.ServerOnly]))
  var password: String = _;

  @BooleanBeanProperty
  var admin: Boolean = _;

  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Public]))
  var currentGameId: Option[ObjectId] = None

  @BooleanBeanProperty
  @JsonView(Array(cUslassOf[JsonViews.ServerOnly]))
  var simple: Boolean = false;

  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var wonGames: Seq[String] = Seq()
  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var lostGames: Seq[String] = Seq()
  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var drawnGames: Seq[String] = Seq()
  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var playedGames: Seq[String] = Seq()

  @JsonProperty
  def stats = {
    if (!isSimple)  Map (
      "won" -> wonGames.length,
      "lost" -> lostGames.length,
      "drawn" -> drawnGames.length,
      "played" -> playedGames.length
    )
    else null
  }


  def validatePassword(password: String) : Boolean = BCrypt.checkpw(password, this.password)


  override def toString = s"User(username: $username, id: $id, admin: $admin)"

  def toSimpleUser: User = {
    val user = new User
    user.setId(id)
    user.setUsername(username)
    user.setSimple(true)
    user
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case user: User if user.getUsername.equals(getUsername) =>
        true
      case _ =>
        false
    }

  }

  def toJson: String = {
    Serializer.serializer.writeValueAsString(this)
  }




}
