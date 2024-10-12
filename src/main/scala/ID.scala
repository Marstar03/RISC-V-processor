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
      val branchType = Output(UInt(3.W))
      val op1Select = Output(UInt(1.W))
      val op2Select = Output(UInt(1.W))
      val ALUop = Output(UInt(4.W))
      val RegA = Output(UInt(32.W))
      val RegB = Output(UInt(32.W))
      val Immediate = Output(SInt(32.W))
      val WBRegAddress = Output(UInt(5.W)) // Adressen til registeret vi vil skrive tilbake til (bit 11 til 7)

      // For forwarding. Need the instruction signal in the EX stage to get the address of the registers that are being read
      val ReadRegAddress1 = Output(UInt(5.W))
      val ReadRegAddress2 = Output(UInt(5.W))
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

  // using the instruction signal to address the two registers, as well as the write register
  registers.io.readAddress1 := io.InstructionSignal.registerRs1
  registers.io.readAddress2 := io.InstructionSignal.registerRs2
  registers.io.writeEnable  := io.ControlSignalsIn.regWrite
  registers.io.writeAddress := io.WBRegAddressIn // kobles til signal fra WB stage
  registers.io.writeData    := io.RegDataIn // kobles til signal fra WB stage

  decoder.instruction := io.InstructionSignal
  io.ControlSignals := decoder.controlSignals
  io.branchType := decoder.branchType
  io.op1Select := decoder.op1Select
  io.op2Select := decoder.op2Select
  io.ALUop := decoder.ALUop

  // Forwarding for readAddress1
  when(io.InstructionSignal.registerRs1 === io.WBRegAddressIn) {
    io.RegA := io.RegDataIn
  } .otherwise {
    io.RegA := registers.io.readData1
  }

  // Forwarding for readAddress2
  when(io.InstructionSignal.registerRs2 === io.WBRegAddressIn) {
    io.RegB := io.RegDataIn
  } .otherwise {
    io.RegB := registers.io.readData2
  }

  //io.RegA := registers.io.readData1
  //io.RegB := registers.io.readData2
  io.WBRegAddress := io.InstructionSignal.registerRd
  io.PCOut := io.PCIn

  io.ReadRegAddress1 := io.InstructionSignal.registerRs1
  io.ReadRegAddress2 := io.InstructionSignal.registerRs2

  
  // finding the right type of immediate format to use, and sign extending it
  // Utvider 12 bit integer til 32 bits ved å duplisere bit 11, altså sign-bit 20 ganger, og legge til de opprinnelige bit-ene
  io.Immediate := MuxLookup(decoder.immType, 0.S, Seq(
    ImmFormat.ITYPE -> Cat(Fill(20, io.InstructionSignal.immediateIType(11)), io.InstructionSignal.immediateIType).asSInt,
    ImmFormat.STYPE -> Cat(Fill(20, io.InstructionSignal.immediateSType(11)), io.InstructionSignal.immediateSType).asSInt,
    //ImmFormat.UTYPE -> Cat(Fill(20, 0.U), io.InstructionSignal.immediateUType) // må fikse denne for at LUI skal funke
    ImmFormat.UTYPE -> Cat(io.InstructionSignal.immediateUType(31, 12), 0.U(12.W)).asSInt, // U-Type upper 20 bits, lower 12 are zeroed
    ImmFormat.JTYPE -> Cat(Fill(12, io.InstructionSignal.immediateJType(19)), io.InstructionSignal.immediateJType).asSInt,
    ImmFormat.BTYPE -> Cat(Fill(20, io.InstructionSignal.immediateBType(11)), io.InstructionSignal.immediateBType).asSInt
  ))

}
