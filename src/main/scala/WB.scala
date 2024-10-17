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
      val InstructionSignalIn = Input(new Instruction)

      val ControlSignalsOut = Output(new ControlSignals)
      val MuxDataOut = Output(UInt(32.W))
      val WBRegAddressOut = Output(UInt(5.W))
      val InstructionSignalOut = Output(new Instruction)
    })

  val MUX = Module(new MyMux).io // mux for å velge mellom ALUIn og MemDataIn til register WB data

  MUX.in0 := io.ALUIn
  MUX.in1 := io.MemDataIn
  MUX.sel := io.ControlSignalsIn.memRead
  
  io.ControlSignalsOut := io.ControlSignalsIn
  io.MuxDataOut := MUX.out
  io.WBRegAddressOut := io.WBRegAddressIn
  io.InstructionSignalOut := io.InstructionSignalIn

}