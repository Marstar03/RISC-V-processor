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
    }
  )

  val MUX = Module(new MyMux).io // mux for å velge mellom RegB og Immediate til ALU
  val ALU = Module(new ALU).io
  val Adder = Module(new ALU).io

  MUX.in0 := io.RegB
  MUX.in1 := io.Immediate
  MUX.sel := io.op2Select

  ALU.op1 := io.RegA
  ALU.op2 := MUX.out
  //ALU.aluOp := ADD // Setter ALUen til å foreløpig gjøre en add
  ALU.aluOp := io.ALUop
  io.ALUOut := ALU.aluResult

  Adder.op1 := io.PCIn
  Adder.op2 := io.Immediate
  Adder.aluOp := ADD
  io.PCPlusOffset := Adder.aluResult

  io.ControlSignalsOut := io.ControlSignalsIn
  io.RegBOut := io.RegB
  io.WBRegAddressOut := io.WBRegAddressIn




}