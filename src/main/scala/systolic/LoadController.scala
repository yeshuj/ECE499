package systolic

import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import chisel3._
import chisel3.internal.firrtl.Width
import freechips.rocketchip.rocket.{HellaCacheReq, HellaCacheResp, M_XRD}
import freechips.rocketchip.tile._

class LoadReq(val dim: Int, val numbank: Int, val a_w: Width) extends Bundle {
  val address = UInt(width = a_w)
  val row = UInt(dim.W)
  val col = UInt(dim.W)
}

class LoadController(val dim: Int, val d_n:Int, val numbank: Int, val a_w: Width)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle() {
    val memReq = Decoupled(new HellaCacheReq())
    val memResp = Flipped(Valid(new HellaCacheResp()))
    val cmd = Flipped(Decoupled(new LoadReq(dim, numbank, a_w)))
    val regbank = Valid(new RegBankMemReq(dim, d_n))
    val complete = Output(Bool())
  })

  val waiting_for_command :: loading_mem :: exec :: Nil = Enum(3)
  val state = RegInit(waiting_for_command)
  io.cmd.ready := (state === waiting_for_command)
  val row = RegInit(dim.U)
  val col = RegInit(dim.U)
  val addr = RegInit(0.U(a_w))
  val row_counter = RegInit(0.U(dim.W))
  val col_counter = RegInit(0.U(dim.W))
  val last_column = col_counter===col-1.U
  val last_row = row_counter===row-1.U
  val read = RegInit(0.U((dim*dim).W))
  val rowLut = VecInit((for (i <- 0 until dim*dim) yield (i.U/col)))
  val colLut = VecInit((for (i <- 0 until dim*dim) yield (i.U%col)))

  io.memReq.bits.no_alloc := true.B
  io.memReq.bits.mask := false.B
  io.memReq.bits.no_xcpt := true.B
  io.memReq.bits.dprv := 0.U

  io.memReq.valid := state===loading_mem && ! ShiftRegister(last_row && last_column, 1)
  io.memReq.bits.addr := addr + (row_counter*col + col_counter)*4.U
  io.memReq.bits.tag := row_counter*col + col_counter
  io.memReq.bits.cmd := M_XRD // perform a load (M_XWR for stores)
  io.memReq.bits.size := log2Ceil(4).U
  io.memReq.bits.signed := false.B
  io.memReq.bits.data := 0.U // we're not performing any stores...
  io.memReq.bits.phys := false.B

  io.regbank.valid := io.memResp.fire()
  io.regbank.bits.row := rowLut(io.memResp.bits.tag)
  io.regbank.bits.col := colLut(io.memResp.bits.tag)
  io.regbank.bits.data := io.memResp.bits.data
  io.regbank.bits.write := true.B

  io.complete := (read === row*col)

  when(io.memResp.fire()){
    read := read+1.U
  }
  when(io.complete || io.cmd.fire()){
    read := 0.U
  }

//  when(io.memReq.fire()) {
    col_counter := Mux(io.memReq.fire(), Mux(last_column, 0.U, col_counter+1.U), col_counter)
    row_counter := Mux(last_column && io.memReq.fire(), row_counter+1.U, row_counter)
    when(last_row && last_column){
      row_counter := 0.U
    }
//  }
  row := Mux(io.cmd.fire(), io.cmd.bits.row, row)
  col := Mux(io.cmd.fire(), io.cmd.bits.col, col)
  addr := Mux(io.cmd.fire(), io.cmd.bits.address, addr)
  switch(state){
    is(waiting_for_command){
      when(io.cmd.fire()) {
        state := loading_mem
      }
    }
    is(loading_mem){
      when(last_row && last_column){
        state := waiting_for_command
      }
    }
  }
}
