case class Universe(values: Array[Array[Boolean]]) {
  override def toString: String = {
    def cellToString(b: Boolean): String =
      if (b) "1"
      else "0"
    values.map(_.map(cellToString).mkString).mkString("\n")
  }
}

object Universe {
  def fromString(s : String): Universe = {
    def toCell(c: Char): Boolean =
      c match {
        case '1' => true
        case '0' => false
      }
    val lines = s.split("\n")
    val un = lines.map(_.stripLineEnd.toArray.map(toCell))
    new Universe(un)
  }
}
