package brahms.model
import scala.beans.{BeanProperty, BooleanBeanProperty}
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.data.annotation.Id
import brahms.util.WithLogging
import com.fasterxml.jackson.annotation.JsonView
import brahms.serializer.JsonViews
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
  var id: String = _;

  @BeanProperty
  var username: String = _;

  @BeanProperty
  @JsonView(Array(classOf[JsonViews.Private]))
  var password: String = _;

  @BooleanBeanProperty
  var admin: Boolean = _;

  def validatePassword(password: String) : Boolean = BCrypt.checkpw(password, this.password)


  override def toString = s"User(username: $username, id: $id, admin: $admin)"

  def toSimpleUser: User = {
    val user = new User
    user.setId(id)
    user.setUsername(username)
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



}
