package brahms.model

import org.scalatest.FunSuite
import play.api.test.FakeApplication
import brahms.test.WithTestExtras
import brahms.serializer.{JsonViews, Serializer}

class UserTest extends FunSuite with WithTestExtras {
  test("encryptPassword") {
    val s = "somepass"
    val encrypted = User.encryptPassword(s)
    val user = new User
    user.setPassword(encrypted)
    assert(user.validatePassword(s))
  }

  test("serialization") {
    val user = new User
    user.setPassword("bla")
    user.setAdmin(true)
    user.setId("12345")
    val json = Serializer.serializer.writeValueAsString(user)
    println(json)
    val newUser = Serializer.serializer.readValue(json, classOf[User])
    assertResult(user.getPassword)(newUser.getPassword)
    assertResult(user.isAdmin)(newUser.isAdmin)
    assertResult(user.getUsername)(newUser.getUsername)

    println(Serializer.serializer.writerWithView(JsonViews.public).writeValueAsString(user))

  }

}
