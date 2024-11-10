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
      val WBRegAddressWB = Input(UInt(5.W))
      val RegDataWB = Input(UInt(32.W))
      val ControlSignalsWB = Input(new ControlSignals)

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
      val Reg1 = Output(UInt(32.W))
      val Reg2 = Output(UInt(32.W))
      val Immediate = Output(SInt(32.W))
      val WBRegAddress = Output(UInt(5.W))

      // For forwarding. Need the instruction signal in the EX stage to get the address of the registers that are being read
      val ReadRegAddress1 = Output(UInt(5.W))
      val ReadRegAddress2 = Output(UInt(5.W))

      val shouldBranchFast = Output(Bool())
      val BranchAddressFast = Output(UInt()) 
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
  registers.io.writeEnable  := io.ControlSignalsWB.regWrite
  registers.io.writeAddress := io.WBRegAddressWB
  registers.io.writeData    := io.RegDataWB

  // decoding the instruction signal
  decoder.instruction := io.InstructionSignal
  io.ControlSignals := decoder.controlSignals
  io.branchType := decoder.branchType
  io.op1Select := decoder.op1Select
  io.op2Select := decoder.op2Select
  io.ALUop := decoder.ALUop

  // forwarding for register 1
  when((io.InstructionSignal.registerRs1 === io.WBRegAddressWB) && (io.ControlSignalsWB.regWrite) && (io.WBRegAddressWB =/= 0.U)) {
    io.Reg1 := io.RegDataWB
  } .otherwise {
    io.Reg1 := registers.io.readData1
  }

  // forwarding for register 2
  when((io.InstructionSignal.registerRs2 === io.WBRegAddressWB) && (io.ControlSignalsWB.regWrite) && (io.WBRegAddressWB =/= 0.U)) {
    io.Reg2 := io.RegDataWB
  } .otherwise {
    io.Reg2 := registers.io.readData2
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

  val Reg1BranchValue = Wire(UInt(32.W))
  val Reg2BranchValue = Wire(UInt(32.W))
  val Reg1Branch = Wire(new ControlSignals)
  val Reg2Branch = Wire(new ControlSignals)

  // forwarding register 1 values for fast branch handling
  when ((io.InstructionSignal.registerRs1 === io.WBRegAddressEX) && (io.ControlSignalsEX.regWrite) && (io.WBRegAddressEX =/= 0.U)) {
    Reg1BranchValue := io.ALUOutEX
    Reg1Branch := io.ControlSignalsEX
  } .elsewhen ((io.InstructionSignal.registerRs1 === io.WBRegAddressMEM) && (io.ControlSignalsMEM.regWrite) && (io.WBRegAddressMEM =/= 0.U)) {
    Reg1BranchValue := io.ALUOutMEM
    Reg1Branch := io.ControlSignalsMEM
  } .elsewhen ((io.InstructionSignal.registerRs1 === io.WBRegAddressWB) && (io.ControlSignalsWB.regWrite) && (io.WBRegAddressWB =/= 0.U)) {
    Reg1BranchValue := io.RegDataWB
    Reg1Branch := io.ControlSignalsWB
  } .otherwise {
    Reg1BranchValue := registers.io.readData1
    Reg1Branch := decoder.controlSignals
  }

  // forwarding register 2 values for fast branch handling
  when ((io.InstructionSignal.registerRs2 === io.WBRegAddressEX) && (io.ControlSignalsEX.regWrite) && (io.WBRegAddressEX =/= 0.U)) {
    Reg2BranchValue := io.ALUOutEX
    Reg2Branch := io.ControlSignalsEX
  } .elsewhen ((io.InstructionSignal.registerRs2 === io.WBRegAddressMEM) && (io.ControlSignalsMEM.regWrite) && (io.WBRegAddressMEM =/= 0.U)) {
    Reg2BranchValue := io.ALUOutMEM
    Reg2Branch := io.ControlSignalsMEM
  } .elsewhen ((io.InstructionSignal.registerRs2 === io.WBRegAddressWB) && (io.ControlSignalsWB.regWrite) && (io.WBRegAddressWB =/= 0.U)) {
    Reg2BranchValue := io.RegDataWB
    Reg2Branch := io.ControlSignalsWB
  } .otherwise {
    Reg2BranchValue := registers.io.readData2
    Reg2Branch := decoder.controlSignals
  }

  // constructing signal for deciding on fast branch handling
  // is true if its either a BEQ or BNE branch and conditions are met
  // if any of the register values are forwarded, we dont want to branch if the corresponding instruction is a memRead instruction,
  // since we then would need to stall, and we wouldnt save any cycles. We then instead handle this case in the EX stage together with the other branch instructions
  io.shouldBranchFast := (((decoder.branchType === branchType.beq) && (Reg1BranchValue === Reg2BranchValue)) || 
                      ((decoder.branchType === branchType.neq) && (Reg1BranchValue =/= Reg2BranchValue))) &&
                      ((!Reg1Branch.memRead) && (!Reg2Branch.memRead))

  val Adder = Module(new Adder).io

  // adding the PC and immediate to create the branch address
  Adder.in0 := io.PCIn.asSInt
  Adder.in1 := io.Immediate
  io.BranchAddressFast := Adder.out.asUInt

}
