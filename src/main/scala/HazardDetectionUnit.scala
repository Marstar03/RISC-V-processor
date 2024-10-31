package FiveStage

import chisel3._

class HazardDetectionUnit extends Module {
  val io = IO(new Bundle {
    val RegWriteMEM = Input(Bool())
    val MemReadMEM = Input(Bool())
    val RegisterRdMEM = Input(UInt(5.W))
    val RegisterRs1EX = Input(UInt(5.W))
    val RegisterRs2EX = Input(UInt(5.W))
    val stall = Output(Bool())
  })

  when(io.MemReadMEM && io.RegWriteMEM &&
       ((io.RegisterRdMEM === io.RegisterRs1EX) || 
        (io.RegisterRdMEM === io.RegisterRs2EX))) {
    io.stall := true.B
  } .otherwise {
    io.stall := false.B
  }
}