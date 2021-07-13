package systolic

import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import chisel3._
import chisel3.internal.firrtl.Width
import freechips.rocketchip.rocket.{HellaCacheIO, M_XRD}
import freechips.rocketchip.tile._



class ExecuteController(val dim: Int, val d_n:Int, val numbank: Int, val a_w: Width)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle() {
    val startExec = Input(Bool())
    val regbankreq = Decoupled(new RegBankReadReq(d_n))
    val regbankreqTrans = Decoupled(new RegBankReadReq(d_n))
  })

  val waiting_for_command :: transpose :: exec :: Nil = Enum(3)
  val state = RegInit(waiting_for_command)

  val counter = Counter(Range(0, dim, 1), state =/= waiting_for_command)
  io.regbankreq.valid := state === exec
  io.regbankreqTrans.valid := state === transpose

  switch(state){
    is(waiting_for_command){
      when(io.startExec) {
        state := transpose
      }
    }
    is(transpose){
      io.regbankreqTrans.bits.row := counter._1
      when(counter._2){
        state := exec
      }
    }
    is(exec){
      io.regbankreq.bits.row := counter._1
      when(counter._2){
        state:= waiting_for_command
      }
    }
  }
}
