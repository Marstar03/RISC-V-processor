package FiveStage
import chisel3._
import chisel3.util.{ BitPat, MuxCase }
import chisel3.experimental.MultiIOModule


class InstructionDecode extends MultiIOModule {

  // Don't touch the test harness
  val testHarness = IO(
    new Bundle {
      val registerSetup = Input(new RegisterSetupSignals)
      val registerPeek  = Output(UInt(32.W))

      val testUpdates   = Output(new RegisterUpdates)
    })


  val io = IO(
    new Bundle {
      /**
        * TODO: Your code here.
        */
      val InstructionSignal = Input(new Instruction)
      val PCIn = Input(UInt()) // burde kanskje sette til 32 bits?

      val PCOut = Output(UInt())
      val RegA = Output(UInt(32.W))
      val RegB = Output(UInt(32.W))
      val ControlSignals = Output(new ControlSignals)
      val Immediate = Output(UInt(32.W))
      val WBRegAddress = Output(UInt(5.W)) // Adressen til registeret vi vil skrive tilbake til (bit 11 til 7)
    }
  )

  val registers = Module(new Registers)
  val decoder   = Module(new Decoder).io


  /**
    * Setup. You should not change this code
    */
  registers.testHarness.setup := testHarness.registerSetup
  testHarness.registerPeek    := registers.io.readData1
  testHarness.testUpdates     := registers.testHarness.testUpdates


  /**
    * TODO: Your code here.
    */
  registers.io.readAddress1 := io.InstructionSignal.registerRs1
  registers.io.readAddress2 := io.InstructionSignal.registerRs2
  registers.io.writeEnable  := false.B // kobles til signal fra WB stage
  registers.io.writeAddress := 0.U // kobles til signal fra WB stage
  registers.io.writeData    := 0.U // kobles til signal fra WB stage

  decoder.instruction := io.InstructionSignal
  io.ControlSignals := decoder.controlSignals
  io.RegA := registers.io.readData1
  io.RegB := registers.io.readData2
  io.WBRegAddress := io.InstructionSignal.registerRd
  io.PCOut := io.PCIn

  //io.Immediate := io.InstructionSignal.immediateIType
  // Må sign extende fra 12 til 32 bits
  io.Immediate := Cat(Fill(20, io.InstructionSignal.immediateIType(11)), io.InstructionSignal.immediateIType)
}
