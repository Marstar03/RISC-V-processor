package FiveStage
import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule


class MemoryFetch() extends MultiIOModule {


  // Don't touch the test harness
  val testHarness = IO(
    new Bundle {
      val DMEMsetup      = Input(new DMEMsetupSignals)
      val DMEMpeek       = Output(UInt(32.W))

      val testUpdates    = Output(new MemUpdates)
    })

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val RegB = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      val invalidInstructionIn = Input(Bool())

      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val MemData = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val invalidInstructionOut = Output(Bool())
    })


  val DMEM = Module(new DMEM)


  /**
    * Setup. You should not change this code
    */
  DMEM.testHarness.setup  := testHarness.DMEMsetup
  testHarness.DMEMpeek    := DMEM.io.dataOut
  testHarness.testUpdates := DMEM.testHarness.testUpdates


  /**
    * Your code here.
    */
  // many signals just passing through
  // also writing/reading memory based on control signal

  // inputting signals into data memory
  DMEM.io.dataIn      := io.RegB
  DMEM.io.dataAddress := io.ALUIn
  DMEM.io.writeEnable := io.ControlSignalsIn.memWrite

  // passing remaining signals to next stage
  io.ControlSignalsOut := io.ControlSignalsIn
  io.ALUOut := io.ALUIn
  io.MemData := DMEM.io.dataOut
  io.WBRegAddressOut := io.WBRegAddressIn
  io.invalidInstructionOut := io.invalidInstructionIn
}
