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
    for(i <- 0 to dim){
      mem(i)(io.load.bits.row) := io.load.bits.data(i)
    }
  }
  when(io.read.req.fire()){
    io.read.resp.valid := true.B
    io.read.resp.bits.row := io.read.req.bits.row
    io.read.resp.bits.data := mem(io.read.req.bits.row)
  }
}