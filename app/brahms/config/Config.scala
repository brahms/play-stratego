package brahms.config

import org.springframework.context.annotation.{ComponentScan, Bean, Configuration}
import com.mongodb._
import play.api.Play
import play.api.Play.current
import brahms.util.WithLogging
import org.jongo.Jongo
import brahms.serializer.Serializer

@Configuration
@ComponentScan
class Config extends WithLogging {


  @Bean
  def uri: MongoClientURI = {
    val uri = Play.configuration.getString("mongo.uri").get
    val clientUri = new MongoClientURI(uri)
    clientUri
  }

  @Bean
  def jongo: Jongo = {
    new Jongo(mongoClient.getDB(uri.getDatabase), Serializer.createJongoMapper)
  }

  @Bean
  def mongoClient: MongoClient = {
    new MongoClient(uri)
  }

}
