package FiveStage

import chisel3._


class IFBarrier() extends Module {

  val io = IO(
    new Bundle {
      val InstructionIn = Input(new Instruction)
      val PCIn = Input(UInt())

      val InstructionOut = Output(new Instruction)
      val PCOut = Output(UInt())
    }
  )


  io.InstructionOut := io.InstructionIn

  val barrierReg = RegInit(0.U(32.W))
  barrierReg := io.PCIn
  io.PCOut := barrierReg
}

class IDBarrier() extends Module {

  val io = IO(
    new Bundle {
      val InstructionIn = Input(new Instruction)
      val PCIn = Input(UInt())

      val InstructionOut = Output(new Instruction)
      val PCOut = Output(UInt())
    }
  )

  val InstructionBarrierReg = RegInit(0.U(32.W))
  InstructionBarrierReg := io.InstructionIn
  io.InstructionOut := InstructionBarrierReg

  val PCBarrierReg = RegInit(0.U(32.W))
  PCBarrierReg := io.PCIn
  io.PCOut := PCBarrierReg
}