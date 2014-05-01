package brahms.serializer

object JsonViews {

  trait Public
  trait Private extends Public
  trait ServerOnly extends Private

  val PUBLIC = classOf[Public]
  val PRIVATE = classOf[Private]
  val SERVER_ONLY = classOf[Private]
}
