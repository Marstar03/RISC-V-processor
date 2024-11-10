package FiveStage
import chisel3._
import chisel3.util.{ BitPat, MuxCase }
import chisel3.experimental.MultiIOModule
import ALUOps._


class Execute extends MultiIOModule {

  val io = IO(
    new Bundle {
      val PCIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)
      val branchType = Input(UInt(3.W))
      val op1Select = Input(UInt(1.W))
      val op2Select = Input(UInt(1.W))
      val ALUop = Input(UInt(4.W))
      val Reg1 = Input(UInt(32.W))
      val Reg2 = Input(UInt(32.W))
      val Immediate = Input(SInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))

      // For forwarding
      val ALUOutMEM = Input(UInt(32.W))
      val WBRegAddressOutMEM = Input(UInt(5.W))
      val MuxDataOutWB = Input(UInt(32.W))
      val WBRegAddressOutWB = Input(UInt(5.W))
      val ReadRegAddress1 = Input(UInt(5.W))
      val ReadRegAddress2 = Input(UInt(5.W))
      val ControlSignalsOutMEM = Input(new ControlSignals)
      val ControlSignalsOutWB = Input(new ControlSignals)
      val invalidInstructionOutMEM = Input(Bool())
      val invalidInstructionOutWB = Input(Bool())

      val BranchAddress = Output(UInt())
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val Reg2Out = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val shouldBranch = Output(Bool())
      val isBranching = Output(Bool())
      val BranchDestination = Output(UInt())
      // For forwarding
      val stall = Output(Bool())
    }
  )

  val op1MUX = Module(new MyMux).io
  val op2MUX = Module(new MyMux).io
  val jumpMUX = Module(new MyMux).io
  val pcplus4MUX = Module(new MyMux).io

  val forwardingUnit1 = Module(new Mux3).io
  val forwardingUnit2 = Module(new Mux3).io

  val ALU = Module(new ALU).io
  val Adder = Module(new Adder).io

  // register som holder tidligere resultat i WB. Brukes dersom vi staller i EX slik at vi ikke mister forwardet verdi fra WB
  val MuxDataOutWBPrev = RegInit(0.U(32.W))
  val MuxDataOutWBPrevAddr = RegInit(0.U(5.W))
  val invalidInstructionWBPrev = RegInit(false.B)

  // unit for checking if any of the read registers are currently being fetched from memory. If so, the stall signal is high, making the IF, ID and EX stages stall
  val hazardDetectionUnit = Module(new HazardDetectionUnit)

  hazardDetectionUnit.io.RegWriteMEM := io.ControlSignalsOutMEM.regWrite
  hazardDetectionUnit.io.MemReadMEM := io.ControlSignalsOutMEM.memRead
  hazardDetectionUnit.io.RegisterRdMEM := io.WBRegAddressOutMEM
  hazardDetectionUnit.io.RegisterRs1EX := io.ReadRegAddress1
  hazardDetectionUnit.io.RegisterRs2EX := io.ReadRegAddress2

  io.stall := hazardDetectionUnit.io.stall


  MuxDataOutWBPrev := io.MuxDataOutWB
  MuxDataOutWBPrevAddr := io.WBRegAddressOutWB
  invalidInstructionWBPrev := io.invalidInstructionOutWB

  // må legge til som condition at vi kun forwarder dersom denne instruksjonen ikke er en nop
  forwardingUnit1.in0 := io.Reg1
  forwardingUnit1.in1 := io.MuxDataOutWB
  forwardingUnit1.in2 := io.ALUOutMEM

  when (((io.ReadRegAddress1 =/= io.WBRegAddressOutMEM) || (io.invalidInstructionOutMEM) || (io.WBRegAddressOutMEM === 0.U) || (!io.ControlSignalsOutMEM.regWrite)) && 
  ((io.ReadRegAddress1 =/= io.WBRegAddressOutWB) || (io.invalidInstructionOutWB) || (io.WBRegAddressOutWB === 0.U) || (!io.ControlSignalsOutWB.regWrite))) {
    forwardingUnit1.sel := 0.U
  } .elsewhen (((io.ReadRegAddress1 =/= io.WBRegAddressOutMEM) || (io.invalidInstructionOutMEM) || (io.WBRegAddressOutMEM === 0.U) || (!io.ControlSignalsOutMEM.regWrite)) && 
  ((io.ReadRegAddress1 === io.WBRegAddressOutWB) && (!io.invalidInstructionOutWB) && (io.WBRegAddressOutWB =/= 0.U) && (io.ControlSignalsOutWB.regWrite))) {
    forwardingUnit1.sel := 1.U
  } .elsewhen ((io.ReadRegAddress1 === io.WBRegAddressOutMEM) && (!io.invalidInstructionOutMEM) && (io.WBRegAddressOutMEM =/= 0.U) && (io.ControlSignalsOutMEM.regWrite)) {
    forwardingUnit1.sel := 2.U
  } .otherwise {
    forwardingUnit1.sel := 3.U
  }

  val StallPrevReg = RegInit(false.B)

  StallPrevReg := io.stall

  // mux to choose between forwardingUnit1.out and PC for the ALU op1
  // If we forwarded register 1 from WB, but we had stall, we now forward the previous WB value. If not, we forward as normal
  when ((StallPrevReg) && (forwardingUnit1.sel === 0.U) && (io.ReadRegAddress1 === MuxDataOutWBPrevAddr) && (!invalidInstructionWBPrev) && (MuxDataOutWBPrevAddr =/= 0.U)) {
    op1MUX.in0 := MuxDataOutWBPrev
  } .otherwise {
    op1MUX.in0 := forwardingUnit1.out
  }

  op1MUX.in1 := io.PCIn
  op1MUX.sel := io.op1Select

  forwardingUnit2.in0 := io.Reg2
  forwardingUnit2.in1 := io.MuxDataOutWB
  forwardingUnit2.in2 := io.ALUOutMEM

  when (((io.ReadRegAddress2 =/= io.WBRegAddressOutMEM) || (io.invalidInstructionOutMEM) || (io.WBRegAddressOutMEM === 0.U) || (!io.ControlSignalsOutMEM.regWrite)) && 
  ((io.ReadRegAddress2 =/= io.WBRegAddressOutWB) || (io.invalidInstructionOutWB) || (io.WBRegAddressOutWB === 0.U) || (!io.ControlSignalsOutWB.regWrite))) {
    forwardingUnit2.sel := 0.U
  } .elsewhen (((io.ReadRegAddress2 =/= io.WBRegAddressOutMEM) || (io.invalidInstructionOutMEM) || (io.WBRegAddressOutMEM === 0.U) || (!io.ControlSignalsOutMEM.regWrite)) && 
  ((io.ReadRegAddress2 === io.WBRegAddressOutWB) && (!io.invalidInstructionOutWB) && (io.WBRegAddressOutWB =/= 0.U) && (io.ControlSignalsOutWB.regWrite))) {
    forwardingUnit2.sel := 1.U
  } .elsewhen ((io.ReadRegAddress2 === io.WBRegAddressOutMEM) && (!io.invalidInstructionOutMEM) && (io.WBRegAddressOutMEM =/= 0.U) && (io.ControlSignalsOutMEM.regWrite)) {
    forwardingUnit2.sel := 2.U
  } .otherwise {
    forwardingUnit2.sel := 3.U
  }

  // mux to choose between forwardingUnit2.out and the immediate for the ALU op 2

  // If we forwarded register 2 from WB, but we had stall, we now forward the previous WB value. If not, we forward as normal
  when ((StallPrevReg) && (forwardingUnit2.sel === 0.U) && (io.ReadRegAddress2 === MuxDataOutWBPrevAddr) && (!invalidInstructionWBPrev) && (MuxDataOutWBPrevAddr =/= 0.U)) {
    op2MUX.in0 := MuxDataOutWBPrev
  } .otherwise {
    op2MUX.in0 := forwardingUnit2.out
  }

  op2MUX.in1 := io.Immediate.asUInt
  op2MUX.sel := io.op2Select
  
  // inputting signals to the ALU
  ALU.op1 := op1MUX.out
  ALU.op2 := op2MUX.out
  ALU.aluOp := io.ALUop

  // mux for choosing between the alu output and pc + 4 for ALUOut output signal
  pcplus4MUX.in0 := ALU.aluResult
  pcplus4MUX.in1 := io.PCIn + 4.U
  pcplus4MUX.sel := io.ControlSignalsIn.jump

  io.ALUOut := pcplus4MUX.out

  // mux to choose between PC and register 1 for adding with the immediate
  jumpMUX.in0 := io.PCIn
  jumpMUX.in1 := forwardingUnit1.out
  jumpMUX.sel := (io.branchType === branchType.jumpReg)
  
  // adder component which adds the output of jumpMUX (register 1 / PC) with the immediate
  Adder.in0 := jumpMUX.out.asSInt
  Adder.in1 := io.Immediate


  io.Reg2Out := forwardingUnit2.out // passing register 2 value to MEM for use in store-instruction

  // passing remaining signals to next stage
  io.ControlSignalsOut := io.ControlSignalsIn
  io.WBRegAddressOut := io.WBRegAddressIn

  val PreviousPCIn = RegInit(0.U(32.W))

  // storing previous PC address for use in shouldBranch and allBranch signals
  when (!hazardDetectionUnit.io.stall) {
    PreviousPCIn := io.PCIn
  } .otherwise {
    PreviousPCIn := 0.U
  }

  val BranchDestinationReg = RegInit(0.U(32.W))

  // shouldbranch is a signal that is true if any of the conditions for branch/jump is satisfied
  // so it decides if we should use the BranchAddress signal for addressing the next instruction
  io.shouldBranch := (io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U) && (io.ControlSignalsOutMEM.memRead || io.ControlSignalsOutWB.memRead)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U) && (io.ControlSignalsOutMEM.memRead || io.ControlSignalsOutWB.memRead)) ||
                  // ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  // ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U)) ||
                  ((io.branchType === branchType.lt) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gte) && (ALU.aluResult === 0.U)) ||
                  ((io.branchType === branchType.ltu) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gteu) && (ALU.aluResult === 0.U))) &&
                  (io.PCIn =/= PreviousPCIn)

  val AllBranching = Wire(new Bool)

  AllBranching := (io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U)) ||
                  ((io.branchType === branchType.lt) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gte) && (ALU.aluResult === 0.U)) ||
                  ((io.branchType === branchType.ltu) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gteu) && (ALU.aluResult === 0.U))) &&
                  (io.PCIn =/= PreviousPCIn)


  when (AllBranching) {
    BranchDestinationReg := io.BranchAddress
  }

  io.BranchDestination := BranchDestinationReg

  io.isBranching := (io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U)) ||
                  ((io.branchType === branchType.lt) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gte) && (ALU.aluResult === 0.U)) ||
                  ((io.branchType === branchType.ltu) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gteu) && (ALU.aluResult === 0.U)))

  // configuration such that the branching address stays the same while we are waiting for the instruction at the target branch address
  when (io.isBranching && (!AllBranching)) {
    // if we have waited for at least one cycle, we use the register to keep same branch address and not get affected by forwarding
    io.BranchAddress := BranchDestinationReg

  } .otherwise {
    when (jumpMUX.sel) {
      // sets the least significant bit to 0 if we add with reg1 value
      io.BranchAddress := Adder.out.asUInt & "hfffffffe".U
    } .otherwise {
      // if just adding with pc we dont do anything
      io.BranchAddress := Adder.out.asUInt
    }
  }

}