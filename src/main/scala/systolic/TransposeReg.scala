package systolic

import chisel3._
import chisel3.util._


class TransposeReg(val dim: Int, val d_n: Int) extends Module {
  val io = IO(new Bundle{
    val sys = Flipped(new RegBankSystolicIO(dim, d_n))
    val load = Flipped(Valid(new RegBankSysResp(dim, d_n)))
  })
  val mem = Reg(Vec(dim, Vec(dim, UInt(d_n.W))))

  when(io.load.fire()){
//    for(i <- 0 until dim){
      mem(io.load.bits.col) := io.load.bits.data
//    }
  }
  io.sys.out.valid := io.sys.cmd.fire() && !io.sys.cmd.bits.write
  io.sys.out.bits.col := io.sys.cmd.bits.col
//  io.sys.out.bits.data := mem(io.sys.cmd.bits.col)
  for(i <- 0 until dim){
    io.sys.out.bits.data(i) := mem(i)(io.sys.cmd.bits.col)
  }
}