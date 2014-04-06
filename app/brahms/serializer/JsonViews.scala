package brahms.serializer

object JsonViews {

  trait Public
  trait Private extends Public

  val public = classOf[Public]
  val priv = classOf[Private]
}
