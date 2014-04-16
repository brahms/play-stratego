package brahms.serializer

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{JacksonModule, DefaultScalaModule}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import org.jongo.Mapper
import org.jongo.marshall.jackson.JacksonMapper
import org.jongo.marshall.jackson.configuration.MapperModifier
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.bson.types.ObjectId
import com.fasterxml.jackson.core.{JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

object Serializer {
  def modifyMapper(o: ObjectMapper) {
    o.registerModule(new DefaultScalaModule)
    o.registerModule(new JodaModule)
    o.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    o.setSerializationInclusion(Include.NON_NULL)
    o.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    o.configure(MapperFeature.INFER_PROPERTY_MUTATORS, true)
    o.setVisibilityChecker(o.getSerializationConfig
      .getDefaultVisibilityChecker
      .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
      .asInstanceOf[VisibilityChecker[_]]
      .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      .asInstanceOf[VisibilityChecker[_]]
      .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
      .asInstanceOf[VisibilityChecker[_]]
    )
    o.configure(SerializationFeature.INDENT_OUTPUT, true)
  }
  def createMapper() = {
    val o = new ObjectMapper()
    modifyMapper(o)
    o
  }

  val serializer = createMapper()
  serializer.registerModule(new SimpleModule {
    addSerializer(new StdSerializer[ObjectId](classOf[ObjectId]) {
      override def serialize(value: ObjectId, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
        jgen.writeString(value.toString)
      }
    })

    addDeserializer(classOf[ObjectId], new StdDeserializer[ObjectId](classOf[ObjectId]) {
      override def deserialize(jp: JsonParser, ctxt: DeserializationContext): ObjectId = {
        new ObjectId(jp.getValueAsString)
      }
    })
  })

  def createJongoMapper: Mapper = {
    val builder = new JacksonMapper.Builder
    builder.addModifier(new MapperModifier {
      override def modify(mapper: ObjectMapper): Unit = {
        modifyMapper(mapper)
      }
    })
    builder.build()
  }
}
