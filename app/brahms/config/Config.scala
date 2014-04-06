package brahms.config

import org.springframework.context.annotation.{ComponentScan, Bean, Configuration}
import com.mongodb.Mongo
import org.springframework.data.mongodb.core.{MongoTemplate, MongoFactoryBean}
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@EnableMongoRepositories
@Configuration
@ComponentScan
class Config {

  @Bean
  def mongo: MongoFactoryBean = {
    val bean = new MongoFactoryBean
    bean.setHost("localhost")
    bean.setPort(27017)
    bean
  }

  @Bean
  def mongoTemplate() : MongoTemplate = {
    return new MongoTemplate(mongo.getObject, "stratego")
  }
}
