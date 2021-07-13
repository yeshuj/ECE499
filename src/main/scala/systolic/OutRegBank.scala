package systolic

import chisel3.util._
import chisel3._

// Interfaces for compute instructions
class OutRegBankReadReq(val dim: Int) extends Bundle {
  val row = UInt(dim.W)
  val col = UInt(dim.W)
}
class OutRegBankReadResp(val dim: Int, val m:Int) extends Bundle {
  val data = UInt(m.W)
}
class OutRegBankReadIO(val dim: Int, val m: Int) extends Bundle {
  val req = Input(new OutRegBankReadReq(dim))
  val resp = Output(new OutRegBankReadResp(dim, m))
}

// Interfaces for load instructions
class OutRegBankLoadReq(val dim: Int, val m:Int) extends Bundle {
  val data = Vec(dim, UInt(m.W))
}

class OutRegBank(val dim: Int, val d_n: Int) extends Module {
  val io = IO(new Bundle{
    val read = new OutRegBankReadIO(dim, d_n)
    val load = Flipped(Valid(new OutRegBankLoadReq(dim, d_n)))
  })
  val mem = Reg(Vec(dim, Vec(dim, UInt(d_n.W))))
  val counter = Counter(io.load.fire(), dim)

  when(io.load.fire()){
    mem(counter._1) := io.load.bits.data
  }
  io.read.resp.data := mem(io.read.req.row)(io.read.req.col)
}

