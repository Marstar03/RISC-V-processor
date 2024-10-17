package FiveStage
import chisel3._
import chisel3.util.{ BitPat, MuxCase }
import chisel3.experimental.MultiIOModule
import ALUOps._


class Execute extends MultiIOModule {

  val io = IO(
    new Bundle {
      val PCIn = Input(UInt()) // burde kanskje sette til 32 bits?
      val ControlSignalsIn = Input(new ControlSignals)
      val branchType = Input(UInt(3.W))
      val op1Select = Input(UInt(1.W))
      val op2Select = Input(UInt(1.W))
      val ALUop = Input(UInt(4.W))
      val RegA = Input(UInt(32.W))
      val RegB = Input(UInt(32.W))
      val Immediate = Input(SInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W)) // Adressen til registeret vi vil skrive tilbake til (bit 11 til 7)

      // For forwarding
      val ALUOutMEM = Input(UInt(32.W))
      val WBRegAddressOutMEM = Input(UInt(5.W))
      val MuxDataOutWB = Input(UInt(32.W))
      val WBRegAddressOutWB = Input(UInt(5.W))
      val ReadRegAddress1 = Input(UInt(5.W))
      val ReadRegAddress2 = Input(UInt(5.W))
      val ControlSignalsOutMEM = Input(new ControlSignals)
      val ControlSignalsPrevMEM = Input(new ControlSignals)
      val MemDataMEM = Input(UInt(32.W))
      val InstructionSignalIn = Input(new Instruction)
      val ControlSignalsOutWB = Input(new ControlSignals)
      val InstructionSignalOutMEM = Input(new Instruction)
      val InstructionSignalOutWB = Input(new Instruction)

      val PCPlusOffset = Output(UInt()) // PC pluss immediate verdi for branching
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val shouldBranch = Output(Bool())
      val InstructionSignalOut = Output(new Instruction)

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


  // mux to choose between PC and RegA for adding with the immediate
  jumpMUX.in0 := io.PCIn
  jumpMUX.in1 := io.RegA
  jumpMUX.sel := (io.branchType === branchType.jumpReg)

  forwardingUnit1.in0 := io.RegA
  forwardingUnit1.in1 := io.MuxDataOutWB
  when (io.ControlSignalsPrevMEM.memRead === false.B) {
    forwardingUnit1.in2 := io.ALUOutMEM
  } .otherwise {
    forwardingUnit1.in2 := io.MemDataMEM
  }
  //forwardingUnit1.in2 := io.ALUOutMEM

  // val nopControlSignals = Wire(new ControlSignals)
  // nopControlSignals := nopControlSignals.nop

  // Sjekker om instruksjonen i EX er en nop
  val isNOPEX = (io.InstructionSignalIn.instruction === "h00000013".U)

  // Sjekker om instruksjonen i MEM er en nop
  val isNOPMEM = (io.InstructionSignalOutMEM.instruction === "h00000013".U)

  // Sjekker om instruksjonen i WB er en nop
  val isNOPWB = (io.InstructionSignalOutWB.instruction === "h00000013".U)

  // må legge til som condition at vi kun forwarder dersom denne instruksjonen ikke er en nop
  when (((io.ReadRegAddress1 =/= io.WBRegAddressOutMEM) && (io.ReadRegAddress1 =/= io.WBRegAddressOutWB)) || isNOPEX || ((isNOPMEM) || (isNOPWB))) {
    forwardingUnit1.sel := 0.U
  } .elsewhen ((io.ReadRegAddress1 =/= io.WBRegAddressOutMEM) && (io.ReadRegAddress1 === io.WBRegAddressOutWB) && (!isNOPWB)) {
    forwardingUnit1.sel := 1.U
  } .elsewhen ((io.ReadRegAddress1 === io.WBRegAddressOutMEM) && (!isNOPMEM)) {
    forwardingUnit1.sel := 2.U
  } .otherwise {
    forwardingUnit1.sel := 3.U
  }

  //forwardingUnit1.sel := 0.U


  // mux to choose between forwardingUnit1.out and PC for the ALU op1
  op1MUX.in0 := forwardingUnit1.out
  op1MUX.in1 := io.PCIn
  op1MUX.sel := io.op1Select


  forwardingUnit2.in0 := io.RegB
  forwardingUnit2.in1 := io.MuxDataOutWB
  forwardingUnit2.in2 := io.ALUOutMEM

  when ((io.ReadRegAddress2 =/= io.WBRegAddressOutMEM) && (io.ReadRegAddress2 =/= io.WBRegAddressOutWB) || isNOPEX || ((isNOPMEM) || (isNOPWB))) {
    forwardingUnit2.sel := 0.U
  } .elsewhen ((io.ReadRegAddress2 =/= io.WBRegAddressOutMEM) && (io.ReadRegAddress2 === io.WBRegAddressOutWB) && (!isNOPWB)) {
    forwardingUnit2.sel := 1.U
  } .elsewhen ((io.ReadRegAddress2 === io.WBRegAddressOutMEM) && (!isNOPMEM)) {
    forwardingUnit2.sel := 2.U
  } .otherwise {
    forwardingUnit2.sel := 3.U
  }

  //forwardingUnit2.sel := 0.U


  // mux to choose between forwardingUnit2.out and the immediate for the ALU op 2
  op2MUX.in0 := forwardingUnit2.out
  op2MUX.in1 := io.Immediate.asUInt
  op2MUX.sel := io.op2Select
  

  ALU.op1 := op1MUX.out
  ALU.op2 := op2MUX.out
  ALU.aluOp := io.ALUop

  // mux for choosing between the alu output and pc + 4 for ALUOut output signal
  pcplus4MUX.in0 := ALU.aluResult
  pcplus4MUX.in1 := io.PCIn + 4.U
  pcplus4MUX.sel := io.ControlSignalsIn.jump

  io.ALUOut := pcplus4MUX.out

  // adder component which adds the output of jumpMUX (regA or pc) with the immediate
  Adder.in0 := jumpMUX.out.asSInt
  Adder.in1 := io.Immediate

  when (jumpMUX.sel) {
    // sets the least significant bit to 0 if we add with regA value
    io.PCPlusOffset := Adder.out.asUInt & "hfffffffe".U
  } .otherwise {
    // if just adding with pc we dont do anything
    io.PCPlusOffset := Adder.out.asUInt
  }

  // signals that we keep to the mem stage
  io.ControlSignalsOut := io.ControlSignalsIn
  // må fikse slik at RegBOut får 
  io.RegBOut := forwardingUnit2.out
  //io.RegBOut := io.RegB
  io.WBRegAddressOut := io.WBRegAddressIn
  io.InstructionSignalOut := io.InstructionSignalIn

  // shouldbranch is a signal that is true if any of the conditions for branch/jump is satisfied
  // so it decides if we should use the pcplusoffset signal for addressing the next instruction
  io.shouldBranch := io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U)) ||
                  ((io.branchType === branchType.lt) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gte) && (ALU.aluResult === 0.U)) ||
                  ((io.branchType === branchType.ltu) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gteu) && (ALU.aluResult === 0.U))



  val hazardDetectionUnit = Module(new HazardDetectionUnit)

  hazardDetectionUnit.io.RegWriteMEM := io.ControlSignalsOutMEM.regWrite
  hazardDetectionUnit.io.MemReadMEM := io.ControlSignalsOutMEM.memRead
  hazardDetectionUnit.io.RegisterRdMEM := io.WBRegAddressOutMEM
  hazardDetectionUnit.io.RegisterRs1EX := io.ReadRegAddress1
  hazardDetectionUnit.io.RegisterRs2EX := io.ReadRegAddress2

  io.stall := hazardDetectionUnit.io.stall

  // when (((forwardingUnit1.sel === 2.U) && (op1MUX.sel === 0.U)) || ((forwardingUnit2.sel === 2.U) && (op2MUX.sel === 0.U))) {
  //   io.stall := false.B
  // } .otherwise {
  //   io.stall := false.B
  // }

}