package systolic

import chisel3.util._
import chisel3._

// Interfaces for compute instructions
class RegBankReadReq(val dim: Int) extends Bundle {
  val row = UInt(dim.W)
}
class RegBankReadResp(val dim: Int, val d_n:Int) extends Bundle {
  val row = UInt(dim.W)
  val data = Vec(dim, UInt(d_n.W))
}
class RegBankReadIO(val dim: Int, val d_n: Int) extends Bundle {
  val req = Valid(new RegBankReadReq(dim))
  val resp = Flipped(Valid(new RegBankReadResp(dim, d_n)))
}
//
//class RegBankConfigSetup(val dim: Int, val d_n: Int) extends Bundle {
//  val row = UInt(dim.W)
//  val col = UInt(dim.W)
//}
//class RegBankConfigSetupInfo(val dim: Int, val d_n: Int) extends Bundle {
//  val row = UInt(dim.W)
//  val col = UInt(dim.W)
//}

// Interfaces for load instructions
class RegBankLoadReq(val dim: Int, val d_n: Int) extends Bundle {
  val row = UInt(dim.W)
  val col = UInt(dim.W)
  val data = UInt(d_n.W)
}

class RegBank(val dim: Int, val d_n: Int) extends Module {
  val io = IO(new Bundle{
    val read = Flipped(new RegBankReadIO(dim, d_n))
    val load = Flipped(Valid(new RegBankLoadReq(dim, d_n)))
  })
  val mem = Reg(Vec(dim, Vec(dim, UInt(d_n.W))))

  when(io.load.fire()){
    mem(io.load.bits.row)(io.load.bits.col) := io.load.bits.data
  }
  when(io.read.req.fire()){
    io.read.resp.valid := true.B
    io.read.resp.bits.row := io.read.req.bits.row
    io.read.resp.bits.data := mem(io.read.req.bits.row)
  }
}
