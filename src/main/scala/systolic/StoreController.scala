package systolic

import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import chisel3._
import chisel3.internal.firrtl.Width
import freechips.rocketchip.rocket.{HellaCacheIO, M_XRD, M_XWR}
import freechips.rocketchip.tile._

class StoreReq(val dim: Int, val a_w: Width) extends Bundle {
  val address = UInt(width = a_w)
  val row = UInt(dim.W)
  val col = UInt(dim.W)
}

class StoreController(val dim: Int, val d_n:Int, val a_w: Width)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle() {
    val memIO = new HellaCacheIO
    val cmd = Flipped(Decoupled(new StoreReq(dim, a_w)))
    val outreg = Flipped(new OutRegBankReadIO(dim, d_n))
    val complete = Output(Bool())
  })

  val waiting_for_command :: storing_mem :: Nil = Enum(3)
  val state = RegInit(waiting_for_command)
  io.cmd.ready := (state === waiting_for_command)
  val row = RegInit(dim.U)
  val col = RegInit(dim.U)
  val addr = RegInit(0.U(a_w))
  val row_counter = RegInit(0.U(dim.W))
  val col_counter = RegInit(0.U(dim.W))
  val last_column = col_counter===dim.U-1.U
  val last_row = row_counter===dim.U-1.U
  val load_a = RegInit(false.B)
  val read = RegInit(0.U((dim*dim).W))

  io.memIO.req.valid := ShiftRegister(state===storing_mem && !(last_row && last_column), 1)
  io.memIO.req.bits.addr := ShiftRegister(addr + row_counter*col + col_counter, 1)
  io.memIO.req.bits.tag := ShiftRegister(row_counter*col + col_counter, 1)
  io.memIO.req.bits.cmd := M_XWR // perform a load (M_XWR for stores)
  io.memIO.req.bits.size := log2Ceil(8).U
  io.memIO.req.bits.signed := false.B
  io.memIO.req.bits.data := io.outreg.resp.data
  io.memIO.req.bits.phys := false.B

  io.complete := (state =/= storing_mem)

  io.outreg.req.row := row_counter
  io.outreg.req.col := col_counter

  when(io.memIO.req.fire()){
    read := read+1.U

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
        state := storing_mem
        row := io.cmd.bits.row
        col := io.cmd.bits.col
        addr := io.cmd.bits.address
      }
    }
    is(storing_mem){
      when(last_row && last_column){
        state := waiting_for_command
      }
    }
  }
}
