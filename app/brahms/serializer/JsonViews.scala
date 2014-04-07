package brahms.serializer

object JsonViews {

  trait Public
  trait Private extends Public

  val PUBLIC = classOf[Public]
  val PRIVATE = classOf[Private]
}
