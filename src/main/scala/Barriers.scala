package FiveStage

import chisel3._


class IFBarrier() extends Module {

  val io = IO(
    new Bundle {
      val InstructionIn = Input(new Instruction)
      val PCIn = Input(UInt())
      val stall = Input(Bool())

      val InstructionOut = Output(new Instruction)
      val PCOut = Output(UInt())
    }
  )

  val barrierReg = RegInit(0.U(32.W))
  val InstructionBarrierReg = RegInit(0.U.asTypeOf(new Instruction))
  val StallPrevReg = RegInit(false.B)

  StallPrevReg := io.stall

  InstructionBarrierReg := io.InstructionIn  

  when (!StallPrevReg) {
    barrierReg := io.PCIn
    io.InstructionOut := io.InstructionIn
  } .otherwise {
    io.InstructionOut := InstructionBarrierReg
  }

  // barrierReg := io.PCIn
  io.PCOut := barrierReg
  // io.InstructionOut := io.InstructionIn
}

class IDBarrier() extends Module {

  val io = IO(
    new Bundle {
      val PCIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)
      val branchTypeIn = Input(UInt(3.W))
      val op1SelectIn = Input(UInt(1.W))
      val op2SelectIn = Input(UInt(1.W))
      val ALUopIn = Input(UInt(4.W))
      val RegAIn = Input(UInt(32.W))
      val RegBIn = Input(UInt(32.W))
      val ImmediateIn = Input(SInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      // For forwarding
      val ReadRegAddress1In = Input(UInt(5.W))
      val ReadRegAddress2In = Input(UInt(5.W))
      val stall = Input(Bool())
      val InstructionSignalIn = Input(new Instruction)

      val PCOut = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val branchTypeOut = Output(UInt(3.W))
      val op1SelectOut = Output(UInt(1.W))
      val op2SelectOut = Output(UInt(1.W))
      val ALUopOut = Output(UInt(4.W))
      val RegAOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val ImmediateOut = Output(SInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))

      // For forwarding
      val ReadRegAddress1Out = Output(UInt(5.W))
      val ReadRegAddress2Out = Output(UInt(5.W))
      val InstructionSignalOut = Output(new Instruction)
    }
  )

  val PCBarrierReg = RegInit(0.U(32.W))
  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val branchTypeBarrierReg = RegInit(0.U(3.W))
  val op1SelectBarrierReg = RegInit(0.U(1.W))
  val op2SelectBarrierReg = RegInit(0.U(1.W))
  val ALUopBarrierReg = RegInit(0.U(4.W))
  val RegABarrierReg = RegInit(0.U(32.W))
  val RegBBarrierReg = RegInit(0.U(32.W))
  val ImmediateBarrierReg = RegInit(0.S(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val ReadRegAddress1BarrierReg = RegInit(0.U(32.W))
  val ReadRegAddress2BarrierReg = RegInit(0.U(32.W))
  val InstructionSignalBarrierReg = RegInit(0.U.asTypeOf(new Instruction))

  //val stallReg = RegInit(0.U.asTypeOf(new Bool))

  // Hvis EX har stall signal, vil vi ta inn 
  //stallReg := io.stall

  when (!io.stall) {
    PCBarrierReg := io.PCIn
    ControlSignalsBarrierReg := io.ControlSignalsIn
    branchTypeBarrierReg := io.branchTypeIn
    op1SelectBarrierReg := io.op1SelectIn
    op2SelectBarrierReg := io.op2SelectIn
    ALUopBarrierReg := io.ALUopIn
    RegABarrierReg := io.RegAIn
    RegBBarrierReg := io.RegBIn
    ImmediateBarrierReg := io.ImmediateIn
    WBRegAddressBarrierReg := io.WBRegAddressIn
    ReadRegAddress1BarrierReg := io.ReadRegAddress1In
    ReadRegAddress2BarrierReg := io.ReadRegAddress2In
    InstructionSignalBarrierReg := io.InstructionSignalIn

  }

  io.PCOut := PCBarrierReg

  io.ControlSignalsOut := ControlSignalsBarrierReg

  io.branchTypeOut := branchTypeBarrierReg

  io.op1SelectOut := op1SelectBarrierReg

  io.op2SelectOut := op2SelectBarrierReg

  io.ALUopOut := ALUopBarrierReg

  io.RegAOut := RegABarrierReg

  io.RegBOut := RegBBarrierReg

  io.ImmediateOut := ImmediateBarrierReg
  
  io.WBRegAddressOut := WBRegAddressBarrierReg

  // For forwarding
  io.ReadRegAddress1Out := ReadRegAddress1BarrierReg

  io.ReadRegAddress2Out := ReadRegAddress2BarrierReg
  io.InstructionSignalOut := InstructionSignalBarrierReg

}

class EXBarrier() extends Module {

  val io = IO(
    new Bundle {
      val PCPlusOffsetIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val RegBIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      val shouldBranchIn = Input(Bool())

      // For forwarding
      val stall = Input(Bool())
      val InstructionSignalIn = Input(new Instruction)

      val PCPlusOffsetOut = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val shouldBranchOut = Output(Bool())
      val InstructionSignalOut = Output(new Instruction)
    }
  )

  val PCPlusOffsetBarrierReg = RegInit(0.U(32.W))
  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val ALUBarrierReg = RegInit(0.U(32.W))
  val RegBBarrierReg = RegInit(0.U(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val shouldBranchBarrierReg = RegInit(0.U.asTypeOf(new Bool))
  val InstructionSignalBarrierReg = RegInit(0.U.asTypeOf(new Instruction))

  //val stallReg = RegInit(0.U.asTypeOf(new Bool))

  // Hvis EX har stall signal, vil vi ta inn instruksjonen og holde den der en ekstra sykel. Fra før putter vi verdiene inn i registre, og henter ut fra registre igjen
  // slik at det blir en delay på en sykel. Legger nå til slik at hvis stall er true i EX nå, er stallReg verdien 0 fra før, så tar inn verdiene i barrier.
  // Neste sykel har stallReg blitt true, så tar ikke inn de nye verdiene fra EX, men beholder de gamle. Tar derimot inn 
  //stallReg := io.stall

  PCPlusOffsetBarrierReg := io.PCPlusOffsetIn
  ALUBarrierReg := io.ALUIn
  RegBBarrierReg := io.RegBIn
  WBRegAddressBarrierReg := io.WBRegAddressIn
  shouldBranchBarrierReg := io.shouldBranchIn
  InstructionSignalBarrierReg := io.InstructionSignalIn

  when (!io.stall) {
    ControlSignalsBarrierReg := io.ControlSignalsIn
  } .otherwise {
    ControlSignalsBarrierReg := ControlSignals.nop
  }
  io.PCPlusOffsetOut := PCPlusOffsetBarrierReg

  io.ControlSignalsOut := ControlSignalsBarrierReg

  io.ALUOut := ALUBarrierReg

  io.RegBOut := RegBBarrierReg
  
  io.WBRegAddressOut := WBRegAddressBarrierReg

  io.shouldBranchOut := shouldBranchBarrierReg
  io.InstructionSignalOut := InstructionSignalBarrierReg
}

class MEMBarrier() extends Module {

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val MemDataIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      // For forwarding
      //val stall = Input(Bool())
      val InstructionSignalIn = Input(new Instruction)

      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val MemDataOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val InstructionSignalOut = Output(new Instruction)
    }
  )

  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val ALUBarrierReg = RegInit(0.U(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val InstructionSignalBarrierReg = RegInit(0.U.asTypeOf(new Instruction))

  //val stallReg = RegInit(0.U.asTypeOf(new Bool))
  //stallReg := io.stall


  //when (!stallReg) {
  ControlSignalsBarrierReg := io.ControlSignalsIn
  ALUBarrierReg := io.ALUIn
  WBRegAddressBarrierReg := io.WBRegAddressIn
  InstructionSignalBarrierReg := io.InstructionSignalIn

  //}

  io.ControlSignalsOut := ControlSignalsBarrierReg

  io.ALUOut := ALUBarrierReg

  io.MemDataOut := io.MemDataIn
  
  io.WBRegAddressOut := WBRegAddressBarrierReg
  io.InstructionSignalOut := InstructionSignalBarrierReg
}