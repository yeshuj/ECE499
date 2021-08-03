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
    val regbankreq = Valid(new RegBankSysReq(dim, d_n))
    val regbankreqTrans = Valid(new RegBankSysReq(dim, d_n))
    val complete = Output(Bool())
  })
  val waiting_for_command :: transpose :: exec :: Nil = Enum(3)
  val state = RegInit(waiting_for_command)
  io.complete := ShiftRegister(io.startExec, 5*dim)
  val counter = Counter(Range(0, dim, 1), state =/= waiting_for_command)

  io.regbankreq.valid := state === exec
  io.regbankreqTrans.valid := state === transpose
  io.regbankreq.bits.col := counter._1
  io.regbankreqTrans.bits.col := counter._1
  io.regbankreq.bits.data := VecInit(Seq.fill(dim)(0.U(d_n.W)))
  io.regbankreqTrans.bits.data := VecInit(Seq.fill(dim)(0.U(d_n.W)))
  io.regbankreq.bits.write := false.B
  io.regbankreqTrans.bits.write := false.B

  switch(state){
    is(waiting_for_command){
      state := Mux(io.startExec, transpose, state)
    }
    is(transpose){
      state := Mux(counter._2, exec, state)
    }
    is(exec){
      state:= Mux(counter._2, waiting_for_command, state)
    }
  }
}
