package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class InstructionFetch extends MultiIOModule {

  // Don't touch
  val testHarness = IO(
    new Bundle {
      val IMEMsetup = Input(new IMEMsetupSignals)
      val PC        = Output(UInt())
    }
  )


  /**
    * TODO: Add input signals for handling events such as jumps

    * TODO: Add output signal for the instruction. 
    * The instruction is of type Bundle, which means that you must
    * use the same syntax used in the testHarness for IMEM setup signals
    * further up.
    */
  val io = IO(
    new Bundle {
      val PCPlusOffsetIn = Input(UInt())
      val ControlSignalsIn = Input(new ControlSignals)

      val PC = Output(UInt())
      val InstructionSignal = Output(new Instruction)
    })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)
  val MUX = Module(new MyMux).io // mux for å velge mellom PC og PCPlusOffsetIn


  /**
    * Setup. You should not change this code
    */
  IMEM.testHarness.setupSignals := testHarness.IMEMsetup
  testHarness.PC := IMEM.testHarness.requestedAddress


  /**
    * TODO: Your code here.
    * 
    * You should expand on or rewrite the code below.
    */
  MUX.in1 := io.PCPlusOffsetIn
  MUX.in0 := PC
  //MUX.sel := 0.U // foreløpig lar vi muxen velge PC hele tiden. Må finne hvilket signal
  MUX.sel := io.ControlSignalsIn.jump

  io.PC := MUX.out
  IMEM.io.instructionAddress := MUX.out

  PC := PC + 4.U

  val instruction = Wire(new Instruction)
  instruction := IMEM.io.instruction.asTypeOf(new Instruction)
  io.InstructionSignal := instruction

  //io.InstructionSignal := IMEM.io.instruction.asTypeOf(new Instruction)


  /**
    * Setup. You should not change this code.
    */
  when(testHarness.IMEMsetup.setup) {
    PC := 0.U
    instruction := Instruction.NOP
  }
}
