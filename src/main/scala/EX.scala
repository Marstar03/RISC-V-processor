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

      val PCPlusOffset = Output(UInt()) // PC pluss immediate verdi for branching
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val shouldBranch = Output(Bool())
    }
  )

  val op1MUX = Module(new MyMux).io
  val op2MUX = Module(new MyMux).io
  val jumpMUX = Module(new MyMux).io
  val pcplus4MUX = Module(new MyMux).io

  val ALU = Module(new ALU).io
  val Adder = Module(new Adder).io

  // mux to choose between RegA and PC for the ALU op1
  op1MUX.in0 := io.RegA
  op1MUX.in1 := io.PCIn
  op1MUX.sel := io.op1Select

  // mux to choose between RegB and the immediate for the ALU op 2
  op2MUX.in0 := io.RegB
  op2MUX.in1 := io.Immediate.asUInt
  op2MUX.sel := io.op2Select

  // mux to choose between PC and RegA for adding with the immediate
  jumpMUX.in0 := io.PCIn
  jumpMUX.in1 := io.RegA
  jumpMUX.sel := (io.branchType === branchType.jumpReg)

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
  io.RegBOut := io.RegB
  io.WBRegAddressOut := io.WBRegAddressIn

  // shouldbranch is a signal that is true if any of the conditions for branch/jump is satisfied
  // so it decides if we should use the pcplusoffset signal for addressing the next instruction
  io.shouldBranch := io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U)) ||
                  ((io.branchType === branchType.lt) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gte) && (ALU.aluResult === 0.U)) ||
                  ((io.branchType === branchType.ltu) && (ALU.aluResult === 1.U)) ||
                  ((io.branchType === branchType.gteu) && (ALU.aluResult === 0.U))

}