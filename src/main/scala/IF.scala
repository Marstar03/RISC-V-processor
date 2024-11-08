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
      val PCPlusOffsetEX = Input(UInt())
      val ControlSignalsEX = Input(new ControlSignals)
      val shouldBranchEX = Input(Bool())
      val isBranchingEX = Input(Bool())

      val PCPlusOffsetID = Input(UInt())
      val ControlSignalsID = Input(new ControlSignals)
      val shouldBranchID = Input(Bool())
      val PCOutID = Input(UInt())

      val stall = Input(Bool())

      val PC = Output(UInt())
      val InstructionSignal = Output(new Instruction)
    })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)
  val InstrMUX = Module(new MyMux).io // mux for å velge mellom PC og PCPlusOffsetIn
  val BranchFastMUX = Module(new MyMux).io 


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

  BranchFastMUX.in0 := io.PCPlusOffsetEX
  BranchFastMUX.in1 := io.PCPlusOffsetID
  BranchFastMUX.sel := (io.shouldBranchID) && (!io.isBranchingEX)
  //BranchFastMUX.sel := ((io.shouldBranchID) && ((!io.isBranchingEX) || ((io.isBranchingEX) && io.PCPlusOffsetEX === io.PCOutID)))

  InstrMUX.in0 := PC
  InstrMUX.in1 := BranchFastMUX.out
  InstrMUX.sel := ((io.shouldBranchID) || (io.shouldBranchEX))

  // InstrMUX.in0 := PC
  // InstrMUX.in1 := io.PCPlusOffsetEX
  // InstrMUX.sel := io.shouldBranchEX

  // the pc output signal will then either remain pc or be updated to the pc + offset
  // when (!io.stall) {
  //   io.PC := InstrMUX.out
  // }
  io.PC := InstrMUX.out
  //io.PC := BranchFastMUX.out
  IMEM.io.instructionAddress := InstrMUX.out
  //IMEM.io.instructionAddress := BranchFastMUX.out


  //PC := pcMUX.out

  when (!io.stall) {
    //PC := pcMUX.out
    PC := InstrMUX.out + 4.U
    //PC := BranchFastMUX.out + 4.U
  }

  // // Register to hold the next PC value
  // val nextPC = RegInit(PC)

  // // Update the nextPC register based on the stall signal
  // when (!io.stall) {
  //   nextPC := pcMUX.out
  // }

  // // Update the PC register with the value from nextPC
  // PC := nextPC

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
