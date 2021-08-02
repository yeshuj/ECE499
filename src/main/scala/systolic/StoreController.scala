package systolic

import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import chisel3._
import chisel3.internal.firrtl.Width
import freechips.rocketchip.rocket.{HellaCacheReq, HellaCacheResp, M_XRD, M_XWR}
import freechips.rocketchip.tile._

class StoreReq(val dim: Int, val a_w: Width) extends Bundle {
  val address = UInt(width = a_w)
  val row = UInt(dim.W)
  val col = UInt(dim.W)
}

class StoreController(val dim: Int, val d_n:Int, val a_w: Width)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle() {
    val memReq = Decoupled(new HellaCacheReq())
    val memResp = Flipped(Valid(new HellaCacheResp()))
    val cmd = Flipped(Decoupled(new StoreReq(dim, a_w)))
    val regbank = new RegBankMemIO(dim, d_n)
    val complete = Output(Bool())
  })

  val waiting_for_command :: storing_mem :: Nil = Enum(2)
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

  io.memReq.bits.no_alloc := true.B
  io.memReq.bits.mask := false.B
  io.memReq.bits.no_xcpt := true.B
  io.memReq.bits.dprv := 0.U

  io.memReq.valid := ShiftRegister(state===storing_mem && !(last_row && last_column), 1)
  io.memReq.bits.addr := ShiftRegister(addr + row_counter*col + col_counter, 1)
  io.memReq.bits.tag := ShiftRegister(row_counter*col + col_counter, 1)
  io.memReq.bits.cmd := M_XWR // perform a load (M_XWR for stores)
  io.memReq.bits.size := log2Ceil(8).U
  io.memReq.bits.signed := false.B
  io.memReq.bits.data := io.regbank.out.bits.data
  io.memReq.bits.phys := false.B

  io.complete := (state =/= storing_mem)

  io.regbank.cmd.valid := state===storing_mem && !(last_row && last_column)
  io.regbank.cmd.bits.row := row_counter
  io.regbank.cmd.bits.col := col_counter
  io.regbank.cmd.bits.data := 0.U
  io.regbank.cmd.bits.write := false.B

  when(io.memReq.fire()){
    read := read+1.U

  }
  when(io.complete){
    read := 0.U
  }

  col_counter := Mux(io.memReq.fire(), Mux(last_column, 0.U, col_counter+1.U), col_counter)
  row_counter := Mux(last_column && io.memReq.fire(), Mux(last_row, 0.U, row_counter+1.U), row_counter)

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
