package systolic

import chisel3.util._
import chisel3._

// Interfaces for compute instructions
class RegBankSysResp(val dim: Int, val d_n:Int) extends Bundle {
  val data = Vec(dim, UInt(d_n.W))
  val col = UInt(dim.W)
}
class RegBankSysReq(val dim: Int, val d_n:Int) extends Bundle {
  val col = UInt(dim.W)
  val data = Vec(dim, UInt(d_n.W))
  val write = Bool()
}
//class RegBankReadIO(val dim: Int, val d_n: Int) extends Bundle {
//  val req = Valid(new RegBankReadReq(dim))
//  val resp = Flipped(Valid(new RegBankReadResp(dim, d_n)))
//}
//// Interfaces for load instructions
//class RegBankSystolicRes(val dim: Int, val d_n: Int) extends Bundle {
//  val row = UInt(dim.W)
//  val data = Vec(dim, UInt(d_n.W))
//}
class RegBankSystolicIO(val dim: Int, val d_n: Int) extends Bundle {
  val cmd = Valid(new RegBankSysReq(dim, d_n))
  val out = Flipped(Valid(new RegBankSysResp(dim, d_n)))
}

// Interfaces for D1 memory
class RegBankMemReq(val dim: Int, val d_n: Int) extends Bundle {
  val row = UInt(dim.W)
  val col = UInt(dim.W)
  val data = UInt(d_n.W)
  val write = Bool()
}
class RegBankMemResp(val d_n: Int) extends Bundle{
  val data = UInt(d_n.W)
}
class RegBankMemIO(val dim: Int, val d_n: Int) extends Bundle {
  val cmd = Valid(new RegBankMemReq(dim, d_n))
  val out = Flipped(Valid(new RegBankMemResp(d_n)))
}

class RegBank(val dim: Int, val d_n: Int) extends Module {
  val io = IO(new Bundle{
    val sys = Flipped(new RegBankSystolicIO(dim, d_n))
    val mem = Flipped(new RegBankMemIO(dim, d_n))
  })
  val mem = Reg(Vec(dim, Vec(dim, UInt(d_n.W))))

  io.sys.out.bits.col := io.sys.cmd.bits.col
  io.sys.out.valid := io.sys.cmd.fire()
//  io.sys.out.bits.data := mem(io.sys.cmd.bits.row)
  for(i <- 0 until dim){
    io.sys.out.bits.data(i) := mem(i)(io.sys.cmd.bits.col)
  }
  io.mem.out.valid := io.mem.cmd.fire() && !io.mem.cmd.bits.write
  io.mem.out.bits.data := mem(io.mem.cmd.bits.row)(io.mem.cmd.bits.col)
  when(io.sys.cmd.fire() && io.sys.cmd.bits.write){
    for(i <- 0 until dim){
      mem(i)(io.sys.cmd.bits.col) := io.sys.cmd.bits.data(i)
    }
//    mem(io.sys.cmd.bits.row) := io.sys.cmd.bits.data
  }.elsewhen(io.mem.cmd.fire() && io.mem.cmd.bits.write){
    mem(io.mem.cmd.bits.row)(io.mem.cmd.bits.col) := io.mem.cmd.bits.data
  }
}
