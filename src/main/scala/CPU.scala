package FiveStage

import chisel3._
import chisel3.core.Input
import chisel3.experimental.MultiIOModule
import chisel3.experimental._


class CPU extends MultiIOModule {

  val testHarness = IO(
    new Bundle {
      val setupSignals = Input(new SetupSignals)
      val testReadouts = Output(new TestReadouts)
      val regUpdates   = Output(new RegisterUpdates)
      val memUpdates   = Output(new MemUpdates)
      val currentPC    = Output(UInt(32.W))
    }
  )

  /**
    You need to create the classes for these yourself
    */
  val IFBarrier  = Module(new IFBarrier)
  val IDBarrier  = Module(new IDBarrier)
  val EXBarrier  = Module(new EXBarrier)
  val MEMBarrier = Module(new MEMBarrier)

  val ID  = Module(new InstructionDecode)
  val IF  = Module(new InstructionFetch)
  val EX  = Module(new Execute)
  val MEM = Module(new MemoryFetch)
  val WB  = Module(new WriteBack) // (You may not need this one?)


  /**
    * Setup. You should not change this code
    */
  IF.testHarness.IMEMsetup     := testHarness.setupSignals.IMEMsignals
  ID.testHarness.registerSetup := testHarness.setupSignals.registerSignals
  MEM.testHarness.DMEMsetup    := testHarness.setupSignals.DMEMsignals

  testHarness.testReadouts.registerRead := ID.testHarness.registerPeek
  testHarness.testReadouts.DMEMread     := MEM.testHarness.DMEMpeek

  /**
    spying stuff
    */
  testHarness.regUpdates := ID.testHarness.testUpdates
  testHarness.memUpdates := MEM.testHarness.testUpdates
  testHarness.currentPC  := IF.testHarness.PC


  /**
    TODO: Your code here
    */
  //IF.io.IMEMsetupSignals := io.setupSignals

  // IFBarrier signals
  IFBarrier.io.PCIn := IF.io.PC
  IFBarrier.io.InstructionIn := IF.io.InstructionSignal
  IFBarrier.io.stall := EX.io.stall
  IFBarrier.io.shouldBranch := EX.io.shouldBranch

  ID.io.PCIn := IFBarrier.io.PCOut
  ID.io.InstructionSignal := IFBarrier.io.InstructionOut

  // IDBarrier signals
  IDBarrier.io.PCIn := ID.io.PCOut
  IDBarrier.io.ControlSignalsIn := ID.io.ControlSignals
  IDBarrier.io.branchTypeIn := ID.io.branchType
  IDBarrier.io.op1SelectIn := ID.io.op1Select
  IDBarrier.io.op2SelectIn := ID.io.op2Select
  IDBarrier.io.ALUopIn := ID.io.ALUop
  IDBarrier.io.RegAIn := ID.io.RegA
  IDBarrier.io.RegBIn := ID.io.RegB
  IDBarrier.io.ImmediateIn := ID.io.Immediate
  IDBarrier.io.WBRegAddressIn := ID.io.WBRegAddress
  IDBarrier.io.ReadRegAddress1In := ID.io.ReadRegAddress1
  IDBarrier.io.ReadRegAddress2In := ID.io.ReadRegAddress2
  IDBarrier.io.stall := EX.io.stall
  IDBarrier.io.PCPlusOffsetEX := EX.io.PCPlusOffset
  IDBarrier.io.isBranching := EX.io.isBranching

  EX.io.PCIn := IDBarrier.io.PCOut
  EX.io.ControlSignalsIn := IDBarrier.io.ControlSignalsOut
  EX.io.branchType := IDBarrier.io.branchTypeOut
  EX.io.op1Select := IDBarrier.io.op1SelectOut
  EX.io.op2Select := IDBarrier.io.op2SelectOut
  EX.io.ALUop := IDBarrier.io.ALUopOut
  EX.io.RegA := IDBarrier.io.RegAOut
  EX.io.RegB := IDBarrier.io.RegBOut
  EX.io.Immediate := IDBarrier.io.ImmediateOut
  EX.io.WBRegAddressIn := IDBarrier.io.WBRegAddressOut
  EX.io.ReadRegAddress1 := IDBarrier.io.ReadRegAddress1Out
  EX.io.ReadRegAddress2 := IDBarrier.io.ReadRegAddress2Out

  // EXBarrier signals
  EXBarrier.io.PCPlusOffsetIn := EX.io.PCPlusOffset
  EXBarrier.io.ControlSignalsIn := EX.io.ControlSignalsOut
  EXBarrier.io.ALUIn := EX.io.ALUOut
  EXBarrier.io.RegBIn := EX.io.RegBOut
  EXBarrier.io.WBRegAddressIn := EX.io.WBRegAddressOut
  EXBarrier.io.shouldBranchIn := EX.io.shouldBranch
  EXBarrier.io.stall := EX.io.stall

  EXBarrier.io.EXShouldNOPCS := IDBarrier.io.EXShouldNOPCS

  MEM.io.PCPlusOffsetIn := EXBarrier.io.PCPlusOffsetOut
  MEM.io.ControlSignalsIn := EXBarrier.io.ControlSignalsOut
  MEM.io.ALUIn := EXBarrier.io.ALUOut
  MEM.io.RegB := EXBarrier.io.RegBOut
  MEM.io.WBRegAddressIn := EXBarrier.io.WBRegAddressOut
  MEM.io.shouldBranchIn := EXBarrier.io.shouldBranchOut
  MEM.io.invalidInstructionIn := EXBarrier.io.invalidInstruction

  // MEMBarrier signals
  MEMBarrier.io.ControlSignalsIn := MEM.io.ControlSignalsOut
  MEMBarrier.io.ALUIn := MEM.io.ALUOut
  MEMBarrier.io.MemDataIn := MEM.io.MemData
  MEMBarrier.io.WBRegAddressIn := MEM.io.WBRegAddressOut
  //MEMBarrier.io.stall := EX.io.stall
  MEMBarrier.io.invalidInstructionIn := MEM.io.invalidInstructionOut

  WB.io.ControlSignalsIn := MEMBarrier.io.ControlSignalsOut
  WB.io.ALUIn := MEMBarrier.io.ALUOut
  WB.io.MemDataIn := MEMBarrier.io.MemDataOut
  WB.io.WBRegAddressIn := MEMBarrier.io.WBRegAddressOut
  WB.io.invalidInstructionIn := MEMBarrier.io.invalidInstructionOut

  // Rest of signals

  // MEM to ID
  ID.io.shouldBranch := MEM.io.shouldBranchOut

  // WB to ID
  ID.io.WBRegAddressIn := WB.io.WBRegAddressOut
  ID.io.RegDataIn := WB.io.MuxDataOut
  ID.io.ControlSignalsIn := WB.io.ControlSignalsOut

  // For forwarding
  
  // MEM to EX
  EX.io.ALUOutMEM := MEM.io.ALUOut
  EX.io.WBRegAddressOutMEM := MEM.io.WBRegAddressOut
  EX.io.ControlSignalsOutMEM := MEM.io.ControlSignalsOut
  EX.io.ControlSignalsPrevMEM := MEM.io.ControlSignalsPrev
  EX.io.MemDataMEM := MEM.io.MemData
  EX.io.invalidInstructionOutMEM := MEM.io.invalidInstructionOut

  // WB to EX
  EX.io.MuxDataOutWB := WB.io.MuxDataOut
  EX.io.WBRegAddressOutWB := WB.io.WBRegAddressOut
  EX.io.ControlSignalsOutWB := WB.io.ControlSignalsOut
  EX.io.invalidInstructionOutWB := WB.io.invalidInstructionOut

  // EX to IF
  IF.io.stall := EX.io.stall

  IF.io.PCPlusOffsetIn := EX.io.PCPlusOffset
  IF.io.ControlSignalsIn := EX.io.ControlSignalsOut
  IF.io.shouldBranchIn := EX.io.shouldBranch

}
