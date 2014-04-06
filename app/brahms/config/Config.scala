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
class Config extends WithLogging{

  @Bean
  def mongoTemplate() : MongoTemplate = {
    val uri = Play.configuration.getString("mongo.uri").get
    val mongoUri = new MongoURI(uri)
    val mongo = new Mongo()

    logger.debug("Got a mongo: " + mongo)
    Play.configuration.getString("mongo.pass") match {
      case Some(pass: String) =>
        val credentials = new UserCredentials(
          Play.configuration.getString("mongo.user").get,
          pass
        )
        return new MongoTemplate(mongo, "stratego", credentials)
      case _ =>
        return new MongoTemplate(mongo, "stratego")
    }
  }

}
