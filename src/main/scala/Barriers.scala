package FiveStage

import chisel3._


class IFBarrier() extends Module {

  val io = IO(
    new Bundle {
      val InstructionIn = Input(new Instruction)
      val PCIn = Input(UInt())
      val stall = Input(Bool())
      val shouldBranch = Input(Bool())

      val InstructionOut = Output(new Instruction)
      val PCOut = Output(UInt())
    }
  )

  val barrierReg = RegInit(0.U(32.W))
  val InstructionBarrierReg = RegInit(0.U.asTypeOf(new Instruction))
  val StallPrevReg = RegInit(false.B)

  StallPrevReg := io.stall

  InstructionBarrierReg := io.InstructionIn

  // only updating barrier if no stall
  when (!io.stall) {
    barrierReg := io.PCIn
  }

  // using a register to delay the instruction signal by one cycle in case of a stall
  when (!StallPrevReg) {
    io.InstructionOut := io.InstructionIn
  } .otherwise {
    io.InstructionOut := InstructionBarrierReg
  }
  
  // passing register value to ID stage
  io.PCOut := barrierReg

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
      val Reg1In = Input(UInt(32.W))
      val Reg2In = Input(UInt(32.W))
      val ImmediateIn = Input(SInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      // For forwarding/branching
      val ReadRegAddress1In = Input(UInt(5.W))
      val ReadRegAddress2In = Input(UInt(5.W))
      val stall = Input(Bool())
      val isBranching = Input(Bool())
      val BranchAddressEX = Input(UInt())
      val Reg1BranchCSMEMReadIn = Input(UInt(1.W))
      val Reg2BranchCSMEMReadIn = Input(UInt(1.W))

      val PCOut = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val branchTypeOut = Output(UInt(3.W))
      val op1SelectOut = Output(UInt(1.W))
      val op2SelectOut = Output(UInt(1.W))
      val ALUopOut = Output(UInt(4.W))
      val Reg1Out = Output(UInt(32.W))
      val Reg2Out = Output(UInt(32.W))
      val ImmediateOut = Output(SInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      // For forwarding
      val ReadRegAddress1Out = Output(UInt(5.W))
      val ReadRegAddress2Out = Output(UInt(5.W))
      val Reg1BranchCSMEMReadOut = Output(UInt(1.W))
      val Reg2BranchCSMEMReadOut = Output(UInt(1.W))

      // signal from IDBarrier directly to EXBarrier
      val EXShouldNOPCS = Output(Bool())
    }
  )

  val PCBarrierReg = RegInit(0.U(32.W))
  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val branchTypeBarrierReg = RegInit(0.U(3.W))
  val op1SelectBarrierReg = RegInit(0.U(1.W))
  val op2SelectBarrierReg = RegInit(0.U(1.W))
  val ALUopBarrierReg = RegInit(0.U(4.W))
  val Reg1BarrierReg = RegInit(0.U(32.W))
  val Reg2BarrierReg = RegInit(0.U(32.W))
  val ImmediateBarrierReg = RegInit(0.S(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val ReadRegAddress1BarrierReg = RegInit(0.U(32.W))
  val ReadRegAddress2BarrierReg = RegInit(0.U(32.W))
  val Reg1BranchCSMEMReadBarrierReg = RegInit(0.U(1.W))
  val Reg2BranchCSMEMReadBarrierReg = RegInit(0.U(1.W))

  // only updating barrier if no stall and we either are not branching or we have reached the branch target
  when ((!io.stall) && ((io.PCIn === io.BranchAddressEX) || (!io.isBranching))) {
    PCBarrierReg := io.PCIn
    ControlSignalsBarrierReg := io.ControlSignalsIn
    branchTypeBarrierReg := io.branchTypeIn
    op1SelectBarrierReg := io.op1SelectIn
    op2SelectBarrierReg := io.op2SelectIn
    ALUopBarrierReg := io.ALUopIn
    Reg1BarrierReg := io.Reg1In
    Reg2BarrierReg := io.Reg2In
    ImmediateBarrierReg := io.ImmediateIn
    WBRegAddressBarrierReg := io.WBRegAddressIn
    ReadRegAddress1BarrierReg := io.ReadRegAddress1In
    ReadRegAddress2BarrierReg := io.ReadRegAddress2In
    Reg1BranchCSMEMReadBarrierReg := io.Reg1BranchCSMEMReadIn
    Reg2BranchCSMEMReadBarrierReg := io.Reg2BranchCSMEMReadIn

  } 

  // passing register values to EX stage
  io.EXShouldNOPCS := ((io.PCIn =/= io.BranchAddressEX) && (io.isBranching))
  io.PCOut := PCBarrierReg
  io.ControlSignalsOut := ControlSignalsBarrierReg
  io.branchTypeOut := branchTypeBarrierReg
  io.op1SelectOut := op1SelectBarrierReg
  io.op2SelectOut := op2SelectBarrierReg
  io.ALUopOut := ALUopBarrierReg
  io.Reg1Out := Reg1BarrierReg
  io.Reg2Out := Reg2BarrierReg
  io.ImmediateOut := ImmediateBarrierReg
  io.WBRegAddressOut := WBRegAddressBarrierReg
  // For forwarding
  io.ReadRegAddress1Out := ReadRegAddress1BarrierReg
  io.ReadRegAddress2Out := ReadRegAddress2BarrierReg
  io.Reg1BranchCSMEMReadOut := Reg1BranchCSMEMReadBarrierReg
  io.Reg2BranchCSMEMReadOut := Reg2BranchCSMEMReadBarrierReg

}

class EXBarrier() extends Module {

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val Reg2In = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      // signal from IDBarrier directly to EXBarrier
      val EXShouldNOPCS = Input(Bool())
      // For forwarding
      val stall = Input(Bool())

      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val Reg2Out = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val invalidInstruction = Output(Bool())
    }
  )

  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val ALUBarrierReg = RegInit(0.U(32.W))
  val Reg2BarrierReg = RegInit(0.U(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val invalidInstructionBarrierReg = RegInit(false.B)

  // passing EX signals to barrier registers
  ALUBarrierReg := io.ALUIn
  Reg2BarrierReg := io.Reg2In
  WBRegAddressBarrierReg := io.WBRegAddressIn

  // if we are stalling the EX stage, we input the instruction from EX, but in order for it not no execute more than once,
  // we set the instruction as invalid and nop the control signals
  when (!io.stall && !io.EXShouldNOPCS) {
    ControlSignalsBarrierReg := io.ControlSignalsIn
    invalidInstructionBarrierReg := false.B
  } .otherwise {
    ControlSignalsBarrierReg := ControlSignals.nop
    invalidInstructionBarrierReg := true.B
  }

  // passing register values to MEM stage
  io.ControlSignalsOut := ControlSignalsBarrierReg
  io.ALUOut := ALUBarrierReg
  io.Reg2Out := Reg2BarrierReg
  io.WBRegAddressOut := WBRegAddressBarrierReg
  io.invalidInstruction := invalidInstructionBarrierReg
}

class MEMBarrier() extends Module {

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val MemDataIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      // For forwarding
      val invalidInstructionIn = Input(Bool())

      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val MemDataOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val invalidInstructionOut = Output(Bool())
    }
  )

  val ControlSignalsBarrierReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val ALUBarrierReg = RegInit(0.U(32.W))
  val WBRegAddressBarrierReg = RegInit(0.U(32.W))
  val invalidInstructionBarrierReg = RegInit(false.B)

  // passing MEM signals to barrier registers
  ControlSignalsBarrierReg := io.ControlSignalsIn
  ALUBarrierReg := io.ALUIn
  WBRegAddressBarrierReg := io.WBRegAddressIn
  invalidInstructionBarrierReg := io.invalidInstructionIn

  // passing register values to WB stage
  io.ControlSignalsOut := ControlSignalsBarrierReg
  io.ALUOut := ALUBarrierReg
  io.MemDataOut := io.MemDataIn
  io.WBRegAddressOut := WBRegAddressBarrierReg
  io.invalidInstructionOut := invalidInstructionBarrierReg
}