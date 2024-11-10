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
      val BranchAddressEX = Input(UInt())
      val shouldBranchEX = Input(Bool())
      val isBranchingEX = Input(Bool())
      val BranchAddressID = Input(UInt())
      val shouldBranchID = Input(Bool())
      val stall = Input(Bool())

      val PC = Output(UInt())
      val InstructionSignal = Output(new Instruction)
    })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)
  val InstrMUX = Module(new MyMux).io // mux for choosing between PC and branching address
  val BranchFastMUX = Module(new MyMux).io // mux for choosing between the branching address from ID and EX

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

  BranchFastMUX.in0 := io.BranchAddressEX
  BranchFastMUX.in1 := io.BranchAddressID
  BranchFastMUX.sel := (io.shouldBranchID) && (!io.isBranchingEX) // choosing ID if not currently branching in EX

  InstrMUX.in0 := PC
  InstrMUX.in1 := BranchFastMUX.out
  InstrMUX.sel := ((io.shouldBranchID) || (io.shouldBranchEX)) // choosing branching address if the branch condition is true in either ID or EX

  io.PC := InstrMUX.out // PC output will be the output of the mux above
  IMEM.io.instructionAddress := InstrMUX.out

  // only updating the PC register if we are not currently stalling
  when (!io.stall) {
    PC := InstrMUX.out + 4.U
  }

  val instruction = Wire(new Instruction)
  instruction := IMEM.io.instruction.asTypeOf(new Instruction)
  io.InstructionSignal := instruction

  /**
    * Setup. You should not change this code.
    */
  when(testHarness.IMEMsetup.setup) {
    PC := 0.U
    instruction := Instruction.NOP
  }
}
