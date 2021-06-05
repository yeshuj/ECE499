package systolic

import scala.util
import Chisel.ShiftRegister
import chisel3._

class systolicBundle(length: Int) extends Bundle {
  val init          = Input(Bool())
  val rst           = Input(Bool())
  val A          = Input(Vec(length, UInt(16.W)))
  val B          = Input(Vec(length, UInt(16.W)))
  val Result         = Output(Vec(length, UInt(32.W)))
  val Valid   = Output(Vec(length, Bool()))
}

class systolic2 extends Module {
  val io = IO(new systolicBundle(2))
  withReset(io.rst) {
    val sysArr = (for (i <- 0 until 2) yield ((for (j <- 0 until 2) yield Module(new mac()).io)))
    for (i <- 0 until 2; j <- 0 until 2) {
      if (i != 0) {
        sysArr(i)(j).b := sysArr(i - 1)(j).out_b
      } else {
        sysArr(i)(j).b := ShiftRegister(io.B(j), j)
      }
      sysArr(i)(j).rst := io.rst

      if (j != 0) {
        sysArr(i)(j).a := sysArr(i)(j - 1).out_a
        sysArr(i)(j).init := sysArr(i)(j - 1).out_init
        sysArr(i)(j).in_Data := sysArr(i)(j - 1).out_Data
        sysArr(i)(j).in_Valid := sysArr(i)(j - 1).out_Valid
      } else {
        sysArr(i)(j).a := ShiftRegister(io.A(i), i)
        sysArr(i)(j).in_Data := 0.U
        sysArr(i)(j).in_Valid := false.B
        if(i != 0) {
          sysArr(i)(j).init := sysArr(i - 1)(j).out_init
        } else {
          sysArr(i)(j).init := io.init
        }
//        sysArr(i)(j).left_bub := 0.B
      }
    }
    io.Result := VecInit((for (i <- 0 until 2) yield sysArr(i)(1).out_Data))
    io.Valid := (for (i <- 0 until 2) yield sysArr(i)(1).out_Valid).toArray
  }
}

class systolic (size: Int) extends Module {
  val io = IO(new systolicBundle(size))
  withReset(io.rst) {
    val sysArr = (for (i <- 0 until size) yield ((for (j <- 0 until size) yield Module(new mac()).io)))
    for (i <- 0 until size; j <- 0 until size) {
      if (i != 0) {
        sysArr(i)(j).b := sysArr(i - 1)(j).out_b
      } else {
        sysArr(i)(j).b := ShiftRegister(io.B(j), j)
      }
      sysArr(i)(j).rst := io.rst

      if (j != 0) {
        sysArr(i)(j).a := sysArr(i)(j - 1).out_a
        sysArr(i)(j).init := sysArr(i)(j - 1).out_init
        sysArr(i)(j).in_Data := sysArr(i)(j - 1).out_Data
        sysArr(i)(j).in_Valid := sysArr(i)(j - 1).out_Valid
      } else {
        sysArr(i)(j).a := ShiftRegister(io.A(i), i)
        sysArr(i)(j).in_Data := 0.U
        sysArr(i)(j).in_Valid := false.B
        if(i != 0) {
          sysArr(i)(j).init := sysArr(i - 1)(j).out_init
        } else {
          sysArr(i)(j).init := io.init
        }
      }
    }
    io.Result := VecInit((for (i <- 0 until size) yield sysArr(i)(size-1).out_Data))
    io.Valid := (for (i <- 0 until size) yield sysArr(i)(size-1).out_Valid).toArray
  }
}