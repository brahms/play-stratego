package brahms.model
import scala.beans.{BeanProperty, BooleanBeanProperty}
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.data.annotation.Id
import brahms.util.WithLogging
import com.fasterxml.jackson.annotation.JsonView
import brahms.serializer.JsonViews
object User extends WithLogging{
  def encryptPassword(password: String): String = {
    val salt = BCrypt.gensalt(10)
    val enc = BCrypt.hashpw(password, salt)
    logger.debug(s"converted $password to $enc")
    enc
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

  def validatePassword(password: String) : Boolean = {
    BCrypt.checkpw(password, this.password)
  }

  override def toString: String = {
    s"User(id: $id, username: $username, password: $password, admin: $admin)"
  }
}
