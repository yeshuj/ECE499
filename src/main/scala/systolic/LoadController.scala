package systolic

import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import chisel3._
import chisel3.internal.firrtl.Width
import freechips.rocketchip.rocket.{HellaCacheIO, M_XRD}
import freechips.rocketchip.tile._

class LoadReq(val dim: Int, val numbank: Int, val a_w: Width) extends Bundle {
  val address = UInt(width = a_w)
  val row = UInt(dim.W)
  val col = UInt(dim.W)
}

class LoadController(val dim: Int, val d_n:Int, val numbank: Int, val a_w: Width)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle() {
    val memIO = new HellaCacheIO
    val cmd = Flipped(Decoupled(new LoadReq(dim, numbank, a_w)))
    val regbank = Valid(new RegBankLoadReq(dim, d_n))
    val complete = Output(Bool())
  })
  val rowLut = VecInit((for (i <- 0 until dim*dim) yield (i%dim).U))
  val colLut = VecInit((for (i <- 0 until dim*dim) yield (i - (i%dim)*dim).U))

  val waiting_for_command :: loading_mem :: exec :: Nil = Enum(4)
  val state = RegInit(waiting_for_command)
  io.cmd.ready := (state === waiting_for_command)
  val row = RegInit(dim.U)
  val col = RegInit(dim.U)
  val addr = RegInit(0.U(a_w))
  val row_counter = RegInit(0.U(dim.W))
  val col_counter = RegInit(0.U(dim.W))
  val last_column = col_counter===dim.U-1.U
  val last_row = row_counter===dim.U-1.U
  val read = RegInit(0.U((dim*dim).W))

  io.memIO.req.valid := state===loading_mem && !(last_row && last_column)
  io.memIO.req.bits.addr := addr + row_counter*col + col_counter
  io.memIO.req.bits.tag := row_counter*col + col_counter
  io.memIO.req.bits.cmd := M_XRD // perform a load (M_XWR for stores)
  io.memIO.req.bits.size := log2Ceil(8).U
  io.memIO.req.bits.signed := false.B
  io.memIO.req.bits.data := 0.U // we're not performing any stores...
  io.memIO.req.bits.phys := false.B
  io.regbank.valid := io.memIO.resp.fire()
  io.complete := (read === row*col)

  when(io.memIO.resp.fire()){
    read := read+1.U
    io.regbank.bits.row := rowLut(io.memIO.resp.bits.tag)
    io.regbank.bits.col := colLut(io.memIO.resp.bits.tag)
    io.regbank.bits.data := io.memIO.resp.bits.data
  }
  when(io.complete){
    read := 0.U
  }

  when(io.memIO.req.fire()) {
    col_counter := Mux(last_column, 0.U, col_counter+1.U)
    row_counter := Mux(last_column, row_counter+1.U, row_counter)
    when(last_row && last_column){
      row_counter := 0.U
    }
  }
  switch(state){
    is(waiting_for_command){
      when(io.cmd.fire()) {
        state := loading_mem
        row := io.cmd.bits.row
        col := io.cmd.bits.col
        addr := io.cmd.bits.address
      }
    }
    is(loading_mem){
      when(last_row && last_column){
        state := waiting_for_command
      }
    }
  }
}
