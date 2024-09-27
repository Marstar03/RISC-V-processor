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
      val Immediate = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W)) // Adressen til registeret vi vil skrive tilbake til (bit 11 til 7)

      val PCPlusOffset = Output(UInt()) // PC pluss immediate verdi for branching
      val ControlSignalsOut = Output(new ControlSignals)
      val ALUOut = Output(UInt(32.W))
      val RegBOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val shouldBranch = Output(Bool())
    }
  )

  val op1MUX = Module(new MyMux).io // mux for å velge mellom RegA og PC til ALU
  val op2MUX = Module(new MyMux).io // mux for å velge mellom RegB og Immediate til ALU
  val ALU = Module(new ALU).io
  val Adder = Module(new ALU).io

  op1MUX.in0 := io.RegA
  op1MUX.in1 := io.PCIn
  op1MUX.sel := io.op1Select

  op2MUX.in0 := io.RegB
  op2MUX.in1 := io.Immediate
  op2MUX.sel := io.op2Select

  ALU.op1 := op1MUX.out
  ALU.op2 := op2MUX.out
  ALU.aluOp := io.ALUop
  io.ALUOut := ALU.aluResult
  //io.ALUZero := (ALU.aluResult === 0.U)

  Adder.op1 := io.PCIn
  Adder.op2 := io.Immediate
  Adder.aluOp := ADD
  io.PCPlusOffset := Adder.aluResult

  io.ControlSignalsOut := io.ControlSignalsIn
  io.RegBOut := io.RegB
  io.WBRegAddressOut := io.WBRegAddressIn

  // lager et signal som er true hvis vi skal bruke PC + imm til neste instruksjon

  //io.shouldBranch := io.ControlSignalsIn.jump | (io.branchType == branchType.beq & (ALU.aluResult == 0.U)) | (io.branchType == branchType.beq & (ALU.aluResult != 0.U))

  io.shouldBranch := io.ControlSignalsIn.jump || 
                  ((io.branchType === branchType.beq) && (ALU.aluResult === 0.U)) || 
                  ((io.branchType === branchType.neq) && (ALU.aluResult =/= 0.U))




}