package FiveStage

import chisel3._
import chisel3.util.MuxLookup
import ALUOps._


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
    SUB    -> (io.op1 - io.op2),
    SLT    -> (io.op1.asSInt < io.op2.asSInt).asUInt,
    SLTU   -> (io.op1 < io.op2),
    SLL    -> (io.op1 << io.op2(4,0)),
    SRA    -> (io.op1.asSInt >> io.op2(4,0)).asUInt,
    SRL    -> (io.op1 >> io.op2(4,0)),
    AND    -> (io.op1 & io.op2),
    OR     -> (io.op1 | io.op2),
    XOR    -> (io.op1 ^ io.op2)
    )

    io.aluResult := MuxLookup(io.aluOp, 0.U(32.W), ALUopMap)
}