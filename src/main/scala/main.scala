package FiveStage

object main {
  def main(args: Array[String]): Unit = {
    chisel3.Driver.execute(Array("--target-dir", "verilog"), () => new Tile())
  }
}
