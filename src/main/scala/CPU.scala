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

  ID.io.PCIn := IFBarrier.io.PCOut
  ID.io.InstructionSignal := IFBarrier.io.InstructionOut

  // IDBarrier signals
  IDBarrier.io.PCIn := ID.io.PCOut
  IDBarrier.io.ControlSignalsIn := ID.io.ControlSignals
  IDBarrier.io.RegAIn := ID.io.RegA
  IDBarrier.io.RegBIn := ID.io.RegB
  IDBarrier.io.ImmediateIn := ID.io.Immediate
  IDBarrier.io.WBRegAddressIn := ID.io.WBRegAddress

  EX.io.PCIn := IDBarrier.io.PCOut
  EX.io.ControlSignalsIn := IDBarrier.io.ControlSignalsOut
  EX.io.RegA := IDBarrier.io.RegAOut
  EX.io.RegB := IDBarrier.io.RegBOut
  EX.io.Immediate := IDBarrier.io.ImmediateOut
  EX.io.WBRegAddressIn := IDBarrier.io.WBRegAddressOut

  // EXBarrier signals
  EXBarrier.io.PCPlusOffsetIn := EX.io.PCPlusOffset
  EXBarrier.io.ControlSignalsIn := EX.io.ControlSignalsOut
  EXBarrier.io.ALUIn := EX.io.ALUOut
  EXBarrier.io.RegBIn := EX.io.RegBOut
  EXBarrier.io.WBRegAddressIn := EX.io.WBRegAddressOut

  MEM.io.PCPlusOffsetIn := EXBarrier.io.PCPlusOffsetOut
  MEM.io.ControlSignalsIn := EXBarrier.io.ControlSignalsOut
  MEM.io.ALUIn := EXBarrier.io.ALUOut
  MEM.io.RegB := EXBarrier.io.RegBOut
  MEM.io.WBRegAddressIn := EXBarrier.io.WBRegAddressOut

  // MEMBarrier signals
  MEMBarrier.io.ControlSignalsIn := MEM.io.ControlSignalsOut
  MEMBarrier.io.ALUIn := MEM.io.ALUOut
  MEMBarrier.io.MemDataIn := MEM.io.MemData
  MEMBarrier.io.WBRegAddressIn := MEM.io.WBRegAddressOut

  WB.io.ControlSignalsIn := MEMBarrier.io.ControlSignalsOut
  WB.io.ALUIn := MEMBarrier.io.ALUOut
  WB.io.MemDataIn := MEMBarrier.io.MemDataOut
  WB.io.WBRegAddressIn := MEMBarrier.io.WBRegAddressOut

  // Rest of signals

  // MEM to IF
  IF.io.PCPlusOffsetIn := MEM.io.PCPlusOffsetOut
  IF.io.ControlSignalsIn := MEM.io.ControlSignalsOut

  // WB to ID
  ID.io.WBRegAddressIn := WB.io.WBRegAddressOut
  ID.io.RegDataIn := WB.io.MuxDataOut
  ID.io.ControlSignalsIn := WB.io.ControlSignalsOut

}
