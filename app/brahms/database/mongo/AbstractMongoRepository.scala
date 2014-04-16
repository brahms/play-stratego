package brahms.database.mongo

import brahms.util.WithLogging
import javax.inject.Inject
import scala.beans.BeanProperty
import org.jongo.Jongo

abstract class AbstractMongoRepository(val jongo: Jongo) extends WithLogging{
}
