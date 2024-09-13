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
      val PCIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)
      val RegAIn = Input(UInt(32.W))
      val RegBIn = Input(UInt(32.W))
      val ImmediateIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      val PCOut = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val RegAOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val ImmediateOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
    }
  )

  val PCBarrierReg = RegInit(0.U(32.W))
  PCBarrierReg := io.PCIn
  io.PCOut := PCBarrierReg

  val ControlSignalsBarrierReg = RegInit(0.U(32.W))
  ControlSignalsBarrierReg := io.ControlSignalsIn
  io.ControlSignalsOut := ControlSignalsBarrierReg

  val RegABarrierReg = RegInit(0.U(32.W))
  RegABarrierReg := io.RegAIn
  io.RegAOut := RegABarrierReg

  val RegBBarrierReg = RegInit(0.U(32.W))
  RegBBarrierReg := io.RegBIn
  io.RegBOut := RegBBarrierReg

  val ImmediateBarrierReg = RegInit(0.U(32.W))
  ImmediateBarrierReg := io.ImmediateIn
  io.ImmediateOut := ImmediateBarrierReg
  
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  WBRegAddressBarrierReg := io.WBRegAddressIn
  io.WBRegAddressOut := WBRegAddressBarrierReg
}

class EXBarrier() extends Module {

  val io = IO(
    new Bundle {
      val PCPlusOffsetIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val RegBIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      val PCPlusOffsetOut = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
    }
  )

  val PCPlusOffsetBarrierReg = RegInit(0.U(32.W))
  PCPlusOffsetBarrierReg := io.PCPlusOffsetIn
  io.PCPlusOffsetOut := PCPlusOffsetBarrierReg

  val ControlSignalsBarrierReg = RegInit(0.U(32.W))
  ControlSignalsBarrierReg := io.ControlSignalsIn
  io.ControlSignalsOut := ControlSignalsBarrierReg

  val ALUBarrierReg = RegInit(0.U(32.W))
  ALUBarrierReg := io.ALUIn
  io.ALUOut := ALUBarrierReg

  val RegBBarrierReg = RegInit(0.U(32.W))
  RegBBarrierReg := io.RegBIn
  io.RegBOut := RegBBarrierReg
  
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  WBRegAddressBarrierReg := io.WBRegAddressIn
  io.WBRegAddressOut := WBRegAddressBarrierReg
}

class MEMBarrier() extends Module {

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val MemDataIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val MemDataOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
    }
  )

  val ControlSignalsBarrierReg = RegInit(0.U(32.W))
  ControlSignalsBarrierReg := io.ControlSignalsIn
  io.ControlSignalsOut := ControlSignalsBarrierReg

  val ALUBarrierReg = RegInit(0.U(32.W))
  ALUBarrierReg := io.ALUIn
  io.ALUOut := ALUBarrierReg

  io.MemDataOut := io.MemDataIn
  
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  WBRegAddressBarrierReg := io.WBRegAddressIn
  io.WBRegAddressOut := WBRegAddressBarrierReg
}