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
      val shouldBranchIn = Input(Bool())

      val PC = Output(UInt())
      val InstructionSignal = Output(new Instruction)
    })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)
  val InstrMUX = Module(new MyMux).io // mux for å velge mellom PC og PCPlusOffsetIn
  val pcMUX = Module(new MyMux).io 


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

  // Mux that chooses the address between the pc and pc/reg-value + an offset
  InstrMUX.in0 := PC
  InstrMUX.in1 := io.PCPlusOffsetIn
  InstrMUX.sel := io.shouldBranchIn

  // the pc output signal will then either remain pc or be updated to the pc + offset
  io.PC := InstrMUX.out
  IMEM.io.instructionAddress := InstrMUX.out

  // created a mux for updating the actual pc for the next instruction. Choosing between pc + 4 and pc + offset + 4
  pcMUX.in0 := PC + 4.U
  pcMUX.in1 := io.PCPlusOffsetIn + 4.U
  pcMUX.sel := io.shouldBranchIn

  PC := pcMUX.out

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
