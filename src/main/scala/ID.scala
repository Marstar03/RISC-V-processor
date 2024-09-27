package FiveStage
import chisel3._
import chisel3.util._
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
      val PCIn = Input(UInt()) // burde kanskje sette til 32 bits?
      val InstructionSignal = Input(new Instruction)
      val WBRegAddressIn = Input(UInt(5.W))
      val RegDataIn = Input(UInt(32.W))
      val ControlSignalsIn = Input(new ControlSignals) 

      val PCOut = Output(UInt())
      val ControlSignals = Output(new ControlSignals)
      val op2Select = Output(UInt(1.W))
      val ALUop = Output(UInt(4.W))
      val RegA = Output(UInt(32.W))
      val RegB = Output(UInt(32.W))
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
  registers.io.writeEnable  := io.ControlSignalsIn.regWrite // OBS! Vil ikke funke pga for mange signaler // kobles til signal fra WB stage
  registers.io.writeAddress := io.WBRegAddressIn // kobles til signal fra WB stage
  registers.io.writeData    := io.RegDataIn // kobles til signal fra WB stage

  decoder.instruction := io.InstructionSignal
  io.ControlSignals := decoder.controlSignals
  io.op2Select := decoder.op2Select
  io.ALUop := decoder.ALUop
  io.RegA := registers.io.readData1
  io.RegB := registers.io.readData2
  io.WBRegAddress := io.InstructionSignal.registerRd
  io.PCOut := io.PCIn

  

  // Utvider 12 bit integer til 32 bits ved å duplisere bit 11, altså sign-bit 20 ganger, og legge til de opprinnelige bit-ene
  io.Immediate := MuxLookup(decoder.immType, 0.U, Seq(
    ImmFormat.ITYPE -> Cat(Fill(20, io.InstructionSignal.immediateIType(11)), io.InstructionSignal.immediateIType),
    ImmFormat.STYPE -> Cat(Fill(20, io.InstructionSignal.immediateSType(11)), io.InstructionSignal.immediateSType),
    //ImmFormat.UTYPE -> Cat(Fill(20, 0.U), io.InstructionSignal.immediateUType) // må fikse denne for at LUI skal funke
    ImmFormat.UTYPE -> io.InstructionSignal.immediateUType.asUInt
  ))

}
