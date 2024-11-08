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
      val PCIn = Input(UInt())
      val InstructionSignal = Input(new Instruction)
      val WBRegAddressIn = Input(UInt(5.W))
      val RegDataIn = Input(UInt(32.W))
      val ControlSignalsIn = Input(new ControlSignals)

      // for fast branch handling
      val WBRegAddressEX = Input(UInt(5.W))
      val ALUOutEX = Input(UInt(32.W))
      val ControlSignalsEX = Input(new ControlSignals)

      val WBRegAddressMEM = Input(UInt(5.W))
      val ALUOutMEM = Input(UInt(32.W))
      val ControlSignalsMEM = Input(new ControlSignals)

      val PCOut = Output(UInt())
      val ControlSignals = Output(new ControlSignals)
      val branchType = Output(UInt(3.W))
      val op1Select = Output(UInt(1.W))
      val op2Select = Output(UInt(1.W))
      val ALUop = Output(UInt(4.W))
      val RegA = Output(UInt(32.W))
      val RegB = Output(UInt(32.W))
      val Immediate = Output(SInt(32.W))
      val WBRegAddress = Output(UInt(5.W))

      // For forwarding. Need the instruction signal in the EX stage to get the address of the registers that are being read
      val ReadRegAddress1 = Output(UInt(5.W))
      val ReadRegAddress2 = Output(UInt(5.W))

      val shouldBranchFast = Output(Bool())
      val PCPlusOffsetFast = Output(UInt()) 
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
  registers.io.writeAddress := io.WBRegAddressIn
  registers.io.writeData    := io.RegDataIn

  decoder.instruction := io.InstructionSignal
  io.ControlSignals := decoder.controlSignals
  io.branchType := decoder.branchType
  io.op1Select := decoder.op1Select
  io.op2Select := decoder.op2Select
  io.ALUop := decoder.ALUop

  // forwarding for register 1
  when((io.InstructionSignal.registerRs1 === io.WBRegAddressIn) && (io.ControlSignalsIn.regWrite) && (io.WBRegAddressIn =/= 0.U)) {
    io.RegA := io.RegDataIn
  } .otherwise {
    io.RegA := registers.io.readData1
  }

  // forwarding for register 2
  when((io.InstructionSignal.registerRs2 === io.WBRegAddressIn) && (io.ControlSignalsIn.regWrite) && (io.WBRegAddressIn =/= 0.U)) {
    io.RegB := io.RegDataIn
  } .otherwise {
    io.RegB := registers.io.readData2
  }

  // retrieving the addresses from the instruction signal
  io.WBRegAddress := io.InstructionSignal.registerRd
  io.ReadRegAddress1 := io.InstructionSignal.registerRs1
  io.ReadRegAddress2 := io.InstructionSignal.registerRs2

  io.PCOut := io.PCIn // passing the PC to next stage

  
  // finding the right type of immediate format to use, and sign extending it
  io.Immediate := MuxLookup(decoder.immType, 0.S, Seq(
    ImmFormat.UTYPE -> Cat(io.InstructionSignal.immediateUType(31, 12), 0.U(12.W)).asSInt,
    ImmFormat.ITYPE -> Cat(Fill(20, io.InstructionSignal.immediateIType(11)), io.InstructionSignal.immediateIType).asSInt,
    ImmFormat.STYPE -> Cat(Fill(20, io.InstructionSignal.immediateSType(11)), io.InstructionSignal.immediateSType).asSInt,
    ImmFormat.JTYPE -> Cat(Fill(12, io.InstructionSignal.immediateJType(19)), io.InstructionSignal.immediateJType).asSInt,
    ImmFormat.BTYPE -> Cat(Fill(20, io.InstructionSignal.immediateBType(11)), io.InstructionSignal.immediateBType).asSInt
  ))

  val RegABranchValue = Wire(UInt(32.W))
  val RegBBranchValue = Wire(UInt(32.W))
  val RegABranch = Wire(new ControlSignals)
  val RegBBranch = Wire(new ControlSignals)

  // forwarding register 1 values for fast branch handling
  when ((io.InstructionSignal.registerRs1 === io.WBRegAddressEX) && (io.ControlSignalsEX.regWrite) && (io.WBRegAddressEX =/= 0.U)) {
    RegABranchValue := io.ALUOutEX
    RegABranch := io.ControlSignalsEX
  } .elsewhen ((io.InstructionSignal.registerRs1 === io.WBRegAddressMEM) && (io.ControlSignalsMEM.regWrite) && (io.WBRegAddressMEM =/= 0.U)) {
    RegABranchValue := io.ALUOutMEM
    RegABranch := io.ControlSignalsMEM
  } .elsewhen ((io.InstructionSignal.registerRs1 === io.WBRegAddressIn) && (io.ControlSignalsIn.regWrite) && (io.WBRegAddressIn =/= 0.U)) {
    RegABranchValue := io.RegDataIn
    RegABranch := io.ControlSignalsIn
  } .otherwise {
    RegABranchValue := registers.io.readData1
    RegABranch := decoder.controlSignals
  }

  // forwarding register 2 values for fast branch handling
  when ((io.InstructionSignal.registerRs2 === io.WBRegAddressEX) && (io.ControlSignalsEX.regWrite) && (io.WBRegAddressEX =/= 0.U)) {
    RegBBranchValue := io.ALUOutEX
    RegBBranch := io.ControlSignalsEX
  } .elsewhen ((io.InstructionSignal.registerRs2 === io.WBRegAddressMEM) && (io.ControlSignalsMEM.regWrite) && (io.WBRegAddressMEM =/= 0.U)) {
    RegBBranchValue := io.ALUOutMEM
    RegBBranch := io.ControlSignalsMEM
  } .elsewhen ((io.InstructionSignal.registerRs2 === io.WBRegAddressIn) && (io.ControlSignalsIn.regWrite) && (io.WBRegAddressIn =/= 0.U)) {
    RegBBranchValue := io.RegDataIn
    RegBBranch := io.ControlSignalsIn
  } .otherwise {
    RegBBranchValue := registers.io.readData2
    RegBBranch := decoder.controlSignals
  }

  // constructing signal for deciding on fast branch handling
  // is true if its either a BEQ or BNE branch and conditions are met
  // if any of the register values are forwarded, we dont want to branch if the corresponding instruction is a memRead instruction,
  // since we then would need to stall, and we wouldnt save any cycles. We then instead handle this case in the EX stage together with the other branch instructions
  io.shouldBranchFast := (((decoder.branchType === branchType.beq) && (RegABranchValue === RegBBranchValue)) || 
                      ((decoder.branchType === branchType.neq) && (RegABranchValue =/= RegBBranchValue))) &&
                      ((!RegABranch.memRead) && (!RegBBranch.memRead))

  val Adder = Module(new Adder).io

  // adding the PC and immediate to create the branch address
  Adder.in0 := io.PCIn.asSInt
  Adder.in1 := io.Immediate
  io.PCPlusOffsetFast := Adder.out.asUInt

}
