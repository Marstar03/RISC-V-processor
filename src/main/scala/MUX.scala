package FiveStage

import chisel3._

class MyMux extends Module {
    val io = IO(new Bundle {
      val in0 = Input(UInt(32.W))
      val in1 = Input(UInt(32.W))
      val sel = Input(Bool())
      val out = Output(UInt(32.W))
    })

    io.out := Mux(io.sel, io.in1, io.in0)
  }