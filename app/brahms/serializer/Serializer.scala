package brahms.serializer

import com.fasterxml.jackson.databind.{SerializationFeature, MapperFeature, DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.introspect.VisibilityChecker

object Serializer {
  def createMapper() = {
    val o = new ObjectMapper()
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
    o
  }

  val serializer = createMapper()
}
