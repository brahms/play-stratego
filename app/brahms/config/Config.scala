package brahms.config

import org.springframework.context.annotation.{ComponentScan, Bean, Configuration}
import com.mongodb.{MongoURI, Mongo}
import org.springframework.data.mongodb.core.{MongoTemplate, MongoFactoryBean}
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import play.api.Play
import play.api.Play.current
import javax.inject.Singleton
import brahms.util.WithLogging

@EnableMongoRepositories
@Configuration
@ComponentScan
class Config extends WithLogging{

  @Bean
  def mongoTemplate() : MongoTemplate = {
    val uri = Play.configuration.getString("mongo.uri").get
    val mongo = new Mongo(new MongoURI(uri))
    logger.debug("Got a mongo: " + mongo)
    return new MongoTemplate(mongo, "stratego")
  }

}
