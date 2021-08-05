package systolic

import scala.util
import Chisel.ShiftRegister
import chisel3._

class macBundle(val d_n: Int) extends Bundle {
  val move          = Input(Bool())
  val calc        = Input(Bool())
  val a          = Input(UInt(d_n.W))
  val b          = Input(UInt(d_n.W))
  val out_init          = Output(Bool())
  val out_a         = Output(UInt(d_n.W))
  val out_b         = Output(UInt(d_n.W))
  val out_Data    = Output(UInt(d_n.W))
  val out_Valid   = Output(Bool())
  val out_calc    = Output(Bool())

  val in_Data = Input(UInt(d_n.W))
  val in_Valid = Input(Bool())
}

/**
 * Compute GCD using subtraction method.
 * Subtracts the smaller from the larger until register y is zero.
 * value in register x is then the GCD
 */
class mac(val d_n: Int) extends Module {
  val io = IO(new macBundle(d_n))

  val acc = RegInit(0.U(d_n.W))
  val out_data = RegInit(0.U(d_n.W))
  val out_val = RegInit(false.B)
  io.out_a := ShiftRegister(io.a, 1)
  io.out_b := ShiftRegister(io.b, 1)
  io.out_init := ShiftRegister(io.move, 1)
  io.out_calc := ShiftRegister(io.calc, 1)
  io.out_Data := out_data
  io.out_Valid := out_val
  when(io.move) {
    acc := 0.U
    out_data := acc
    out_val := true.B
  }.elsewhen(io.calc){
    acc := acc + io.a * io.b
    out_data := ShiftRegister(io.in_Data, 1)
    out_val := ShiftRegister(io.in_Valid, 1)
  }.otherwise {
    acc := acc
    out_data := ShiftRegister(io.in_Data, 1)
    out_val := ShiftRegister(io.in_Valid, 1)
  }


}
