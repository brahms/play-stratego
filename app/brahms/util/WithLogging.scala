package brahms.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.annotation.JsonIgnore

trait WithLogging {
  @JsonIgnore
  val logger = LoggerFactory.getLogger(getClass)
}