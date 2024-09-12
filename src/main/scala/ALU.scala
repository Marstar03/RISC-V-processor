package FiveStage

import chisel3._


class ALU() extends Module {

  val io = IO(
    new Bundle {
      val op1 = Input(UInt(32.W))
      val op2 = Input(UInt(32.W))
      val aluOp = Input(UInt(32.W))

      val aluResult = Output(UInt(32.W))
    }
  )


  val ALUopMap = Array(
    ADD    -> (io.op1 + io.op2),
    SUB    -> (io.op1 - io.op2)
    )

    io.aluResult := MuxLookup(io.aluOp, 0.U(32.W), ALUopMap)
}