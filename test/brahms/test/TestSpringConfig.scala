package brahms.test

import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import com.mongodb.{MongoClient, MongoClientURI}
import brahms.util.WithLogging
import org.jongo.Jongo
import brahms.serializer.Serializer

@Configuration
@ComponentScan(basePackages = Array("brahms"))
class TestSpringConfig extends WithLogging {

  @Bean
  def uri: MongoClientURI = {
    val clientUri = new MongoClientURI("mongodb://cbrahms:OneOne11@localhost/stratego")
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
