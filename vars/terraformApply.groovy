def call(opts = [:])) {
  def tf = new Terraform(opts)

  return tf.apply()
}
