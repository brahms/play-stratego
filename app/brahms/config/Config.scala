package brahms.config

import org.springframework.context.annotation.{ComponentScan, Bean, Configuration}
import com.mongodb.{MongoCredential, MongoURI, Mongo}
import org.springframework.data.mongodb.core.{MongoTemplate, MongoFactoryBean}
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import play.api.Play
import play.api.Play.current
import javax.inject.Singleton
import brahms.util.WithLogging
import org.springframework.data.authentication.UserCredentials

@EnableMongoRepositories
@Configuration
@ComponentScan
class Config extends WithLogging {

  @Bean
  def mongoTemplate(): MongoTemplate = {
    val uri = Play.configuration.getString("mongo.uri").get
    logger.info("MONGO uri: " + uri)
    val mongoUri = new MongoURI(uri)
    val mongo = new Mongo(mongoUri)
    return new MongoTemplate(mongo, "stratego")
  }

}
