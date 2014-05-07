package brahms.model

import org.scalatest.FunSuite
import play.api.test.FakeApplication
import brahms.test.WithTestExtras
import brahms.serializer.{JsonViews, Serializer}
import scala.xml.Utility
import org.bson.types.ObjectId

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
    user.setCurrentGameId(Some(new ObjectId().toString))
    val json = Serializer.serializer.writeValueAsString(user)
    println(json)
    val newUser = Serializer.serializer.readValue(json, classOf[User])
    assertResult(user.getPassword)(newUser.getPassword)
    assertResult(user.isAdmin)(newUser.isAdmin)
    assertResult(user.getUsername)(newUser.getUsername)

    println(Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(user))

  }

  test("regex") {
    val username = "should12"
    assert(User.validateUsername(username))
    val bad = "1abc32"
    assert(!User.validateUsername(bad))
  }

  test("htmlencode") {
    val user = new User
    user.setPassword("bla")
    user.setAdmin(true)
    println(Utility.escape(Serializer.serializer.writerWithView(JsonViews.PUBLIC).writeValueAsString(user)))
  }

  test("deserialization") {
    val json = """|   {
                 |        "username": "local1",
                 |        "admin": false,
                 |        "_id": {
                 |            "$oid": "536874f9b968ce471508a318"
                 |        },
                 |        "simple": true,
                 |        "wonGames": [],
                 |        "lostGames": [],
                 |        "drawnGames": [],
                 |        "playedGames": []
                 |    }""".stripMargin

    val user = Serializer.serializer.readValue(json, classOf[User])
    assert(user != null)
  }
}
