package systolic

import scala.util
import Chisel.ShiftRegister
import chisel3._

class macBundle() extends Bundle {
  val init          = Input(Bool())
  val a          = Input(UInt(16.W))
  val b          = Input(UInt(16.W))
  val out_init          = Output(Bool())
  val out_a         = Output(UInt(16.W))
  val out_b         = Output(UInt(16.W))
  val out_Data    = Output(UInt(32.W))
  val out_Valid   = Output(Bool())

  val in_Data = Input(UInt(32.W))
  val in_Valid = Input(Bool())
}

/**
 * Compute GCD using subtraction method.
 * Subtracts the smaller from the larger until register y is zero.
 * value in register x is then the GCD
 */
class mac() extends Module {
  val io = IO(new macBundle())

  val acc = RegInit(0.U(64.W))
  val out_data = RegInit(0.U(64.W))
  val out_val = RegInit(false.B)
  io.out_a := ShiftRegister(io.a, 1)
  io.out_b := ShiftRegister(io.b, 1)
  io.out_init := ShiftRegister(io.init, 1)
  io.out_Data := out_data
  io.out_Valid := out_val
  when(io.init) {
    acc := io.a * io.b
    out_data := acc
    out_val := true.B
  }.otherwise {
    acc := acc + io.a * io.b
    out_data := ShiftRegister(io.in_Data, 1)
    out_val := ShiftRegister(io.in_Valid, 1)
  }

}
