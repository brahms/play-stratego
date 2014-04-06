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

  def mongoFromUri(uri: String): Mongo = {
    new Mongo(new MongoURI(uri))
  }

  @Bean
  def mongoFactory: MongoFactoryBean = {
    val bean = new MongoFactoryBean
    bean.setHost("localhost")
    bean.setPort(27017)
    bean
  }

  @Bean
  def mongoTemplate() : MongoTemplate = {
    var mongo: Mongo = null
    Play.configuration.getString("mongodb.uri") match {
      case Some(uri) =>
        logger.debug("Using mongo uri: "+ uri)
        mongo = mongoFromUri(uri)
      case _ =>
        mongo = mongoFactory.getObject
    }
    logger.debug("Got a mongo: " + mongo)
    return new MongoTemplate(mongo, "stratego")
  }

}
