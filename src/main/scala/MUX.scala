package FiveStage

import chisel3._

class Mux2 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(32.W))
    val in1 = Input(UInt(32.W))
    val sel = Input(Bool())
    val out = Output(UInt(32.W))
  })

  io.out := Mux(io.sel, io.in1, io.in0)
}

class Adder extends Module {
  val io = IO(new Bundle {
    val in0 = Input(SInt(32.W))
    val in1 = Input(SInt(32.W))
    val out = Output(SInt(32.W))
  })

  io.out := io.in0 + io.in1
}

class Mux3 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(32.W))
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(32.W))
  })

  io.out := Mux(io.sel === 0.U, io.in0, Mux(io.sel === 1.U, io.in1, io.in2))
}