package systolic

import chisel3._
import chisel3.util._


class TransposeReg(val dim: Int, val d_n: Int) extends Module {
  val io = IO(new Bundle{
    val read = Flipped(new RegBankReadIO(dim, d_n))
    val load = Flipped(Valid(new RegBankReadResp(dim, d_n)))
  })
  val mem = Reg(Vec(dim, Vec(dim, UInt(d_n.W))))

  when(io.load.fire()){
    for(i <- 0 until dim){
      mem(i)(io.load.bits.row) := io.load.bits.data(i)
    }
  }
  io.read.resp.valid := io.read.req.fire()
  io.read.resp.bits.row := io.read.req.bits.row
  io.read.resp.bits.data := mem(io.read.req.bits.row)
}