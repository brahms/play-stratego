package brahms.model.stratego

import org.scalatest.FunSuite
import brahms.model.User
import org.bson.types.ObjectId
import brahms.model.stratego.StrategoActions.CommitAction
import brahms.serializer.Serializer

class CommitActionTest extends FunSuite {

  test("serialization") {
    val user  = new User
    user.setUsername("bla")
    user.setAdmin(false)

    var action: StrategoAction = CommitAction().withUser(user).asInstanceOf[CommitAction]

    val json = Serializer.serializer.writeValueAsString(action)
    println (json)
    action = Serializer.serializer.readValue(json, classOf[StrategoAction])
    assert(true)
  }

}

