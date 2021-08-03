package systolic

import chisel3.Flipped

import scala.util
//import Chisel._
import chisel3._
import chisel3.util._

class systolicBundle(length: Int) extends Bundle {
  val init          = Input(Bool())
  val rst           = Input(Bool())
  val A          = Input(Vec(length, UInt(16.W)))
  val B          = Input(Vec(length, UInt(16.W)))
  val Result         = Output(Vec(length, UInt(32.W)))
  val Valid   = Output(Vec(length, Bool()))
}

class systolic_d(val dim: Int, val d_n: Int) extends Module {

  val io = IO(new Bundle(){
    val calc           = Input(Bool())
    val in_A          = Input(new RegBankSysResp(dim, d_n))
    val in_B          = Input(new RegBankSysResp(dim, d_n))
    val out           = Valid(new RegBankSysReq(dim, d_n))
  })
  val calc_r = RegInit(0.U)
  calc_r := io.calc
  val init = calc_r ^ io.calc
//  val io = IO(new systolicBundle(2))
  val sysArr = (for (i <- 0 until dim) yield ((for (j <- 0 until dim) yield Module(new mac()).io)))
  for (i <- 0 until dim; j <- 0 until dim) {
    if (i != 0) {
      sysArr(i)(j).b := sysArr(i - 1)(j).out_b

    } else {
      sysArr(i)(j).b := ShiftRegister(io.in_B.data(j), j)
    }

    if (j != 0) {
      sysArr(i)(j).a := sysArr(i)(j - 1).out_a
      sysArr(i)(j).init := sysArr(i)(j - 1).out_init
      sysArr(i)(j).in_Data := sysArr(i)(j-1).out_Data
      sysArr(i)(j).in_Valid := sysArr(i)(j-1).out_Valid
    } else {
      sysArr(i)(j).a := ShiftRegister(io.in_A.data(i), i)
      sysArr(i)(j).in_Data := 0.U
      sysArr(i)(j).in_Valid := false.B
      if(i != 0) {
        sysArr(i)(j).init := sysArr(i - 1)(j).out_init
      } else {
        sysArr(i)(j).init := init
      }
//        sysArr(i)(j).left_bub := 0.B
    }
  }
  for (i <- 0 until dim){
    io.out.bits.data(i) := ShiftRegister(sysArr(i)(dim-1).out_Data, dim-1-i)
  }
  val counter = Counter(Range(0, dim, 1), sysArr(dim-1)(dim-1).out_Valid)
  io.out.bits.col := dim.U-counter._1-1.U
  io.out.bits.write := true.B
//  io.out.bits.data := VecInit((for (i <- 0 until dim) yield sysArr(i)(dim-1).out_Data))
  io.out.valid := sysArr(dim-1)(dim-1).out_Valid
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