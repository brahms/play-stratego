package brahms.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.annotation.JsonIgnore
import java.beans.Transient

trait WithLogging {
  @JsonIgnore
  @Transient
  val logger = LoggerFactory.getLogger(getClass)
}