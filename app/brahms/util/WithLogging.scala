package brahms.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait WithLogging {
  val logger = LoggerFactory.getLogger(getClass)
}