package FiveStage
import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule

class WriteBack() extends MultiIOModule {

  val io = IO(
    new Bundle {
      val ControlSignalsIn = Input(new ControlSignals)
      val ALUIn = Input(UInt(32.W))
      val MemDataIn = Input(UInt(32.W))
      val WBRegAddressIn = Input(UInt(5.W))
      val invalidInstructionIn = Input(Bool())

      val ControlSignalsOut = Output(new ControlSignals)
      val MuxDataOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val invalidInstructionOut = Output(Bool())
    })

  val MUX = Module(new MyMux).io

  // choosing between ALU output and memory output to write to register 
  MUX.in0 := io.ALUIn
  MUX.in1 := io.MemDataIn
  MUX.sel := io.ControlSignalsIn.memRead
  
  io.ControlSignalsOut := io.ControlSignalsIn
  io.MuxDataOut := MUX.out
  io.WBRegAddressOut := io.WBRegAddressIn
  io.invalidInstructionOut := io.invalidInstructionIn

}