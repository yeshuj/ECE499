package ECE499

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.tile._
import freechips.rocketchip.rocket._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import systolic._


class Accelerator(opcodes: OpcodeSet, val n: Int = 4)
                       (implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AcceleratorModule(this, n)
}

class AcceleratorModule(outer: Accelerator, dim: Int)
  extends LazyRoCCModuleImp(outer) {
  val busy = RegInit(VecInit(Seq.fill(outer.n){false.B}))
  val cmd = Queue(io.cmd)
  // The parts of the command are as follows
  // inst - the parts of the instruction itself
  //   opcode
  //   rd - destination register number
  //   rs1 - first source register number
  //   rs2 - second source register number
  //   funct
  //   xd - is the destination register being used?
  //   xs1 - is the first source register being used?
  //   xs2 - is the second source register being used?
  // rs1 - the value of source register 1
  // rs2 - the value of source register 2

  val rst :: waiting_for_command :: start_load :: loading :: transpose :: compute :: store_data :: Nil = Enum(7)
  val state = RegInit(waiting_for_command)

  cmd.ready := state === waiting_for_command
  val funct = cmd.bits.inst.funct
  val addr = cmd.bits.rs1(log2Up(outer.n)-1,0)
  val set = funct === 0.U
//  val set_j = funct === 1.U
//  val set_k = funct === 2.U
  val load = funct === 3.U
  val calc = funct === 5.U
  val store = funct === 6.U
  val memRespTag = io.mem.resp.bits.tag(log2Up(outer.n)-1,0)
  val dimI = RegInit(dim.U)
  val dimJ = RegInit(dim.U)
  val dimK = RegInit(dim.U)
//  val isA = RegInit(true.B)
  val baseAddress = RegInit(0.U(64.W))
  val bank_num_0 = RegInit(0.U(64.W))
  val bank_num = RegInit(0.U(64.W))
  val res_bank = RegInit(0.U(64.W))

  val num_reg = 2

  val load_ctrl = Module(new LoadController(dim, 64, num_reg, 64.W))
  val exec_ctrl = Module(new ExecuteController(dim, 64, num_reg, 64.W))
  val str_ctrl = Module(new StoreController(dim, 64, 64.W))
  val reg_bank_seq = (for (i <- 0 until dim)  yield Module(new RegBank(dim, 64)))
  val reg_bank = VecInit(reg_bank_seq.map(_.io))
  val out_reg = Module(new OutRegBank(dim, 64))
  val systolic = Module(new systolic_d(dim, 64))
  val regbankrow = RegInit(VecInit(Seq.fill(num_reg)(0.U(dim.W)))) // TODO: log2up(dim)
  val regbankcol = RegInit(VecInit(Seq.fill(num_reg)(0.U(dim.W))))
  val transpose_reg = Module(new TransposeReg(dim, 64))

  load_ctrl.io.cmd.valid := state === start_load
  load_ctrl.io.cmd.bits.row := regbankrow(bank_num_0)
  load_ctrl.io.cmd.bits.col := regbankcol(bank_num_0)
//  load_ctrl.io.cmd.bits.isA := isA
  load_ctrl.io.cmd.bits.address := baseAddress

  exec_ctrl.io.startExec := state === compute

  for(i<- 0 until dim){
    when(i.U===bank_num_0){
      reg_bank(i).read.req <> exec_ctrl.io.regbankreq
    }.otherwise{
      reg_bank(i).read.req <> exec_ctrl.io.regbankreqTrans
    }
    reg_bank(i).load.bits := load_ctrl.io.regbank.bits
    reg_bank(i).load.valid := Mux(i.U===bank_num_0, load_ctrl.io.regbank.valid, false.B)
//    reg_bank(i).read.req <> Mux(i.U===bank_num, exec_ctrl.io.regbankreqTrans, exec_ctrl.io.regbankreq)
  }

  transpose_reg.io.load <> reg_bank(bank_num).read.resp

//  when(exec_ctrl.io.regbankreqTrans.fire()){
//    reg_bank(bank_num).read.req <> exec_ctrl.io.regbankreqTrans
//    transpose_reg.io.load <> reg_bank(bank_num).read.resp
//  }
//  reg_bank(bank_num_0).read.req <> exec_ctrl.io.regbankreq
  transpose_reg.io.read.req <> exec_ctrl.io.regbankreq
  systolic.io.in_A <> reg_bank(bank_num_0).read.resp.bits
  systolic.io.in_B <> transpose_reg.io.read.resp.bits
  systolic.io.calc := exec_ctrl.io.regbankreq.valid

  out_reg.io.load <> systolic.io.out

  str_ctrl.io.cmd.valid := state === store_data
  str_ctrl.io.cmd.bits.col := dimJ
  str_ctrl.io.cmd.bits.row := dimI
  str_ctrl.io.cmd.bits.address := baseAddress
  out_reg.io.read<>str_ctrl.io.outreg

  when(state === loading){
    io.mem.req.bits <> load_ctrl.io.memReq.bits
  }.otherwise {
    io.mem.req.bits <> str_ctrl.io.memReq.bits
  }
  io.mem.req.valid := load_ctrl.io.memReq.valid || str_ctrl.io.memReq.valid
  load_ctrl.io.memReq.ready := io.mem.req.ready && state === loading
  str_ctrl.io.memReq.ready := io.mem.req.ready && state === loading

  load_ctrl.io.memResp <> io.mem.resp
  str_ctrl.io.memResp <> io.mem.resp

//  when(state === loading){
//    io.mem.req.bits <> load_ctrl.io.memReq.bits
//  }.otherwise {
//    io.mem.req.bits := str_ctrl.io.memReq.bits
//  }
//  io.mem.req.valid := load_ctrl.io.memReq.valid || str_ctrl.io.memReq.valid
//  load_ctrl.io.memReq.ready :=


  io.cmd.ready := (state === waiting_for_command)
  when(set){
    regbankrow(io.cmd.bits.rs1) := io.cmd.bits.rs2(36,32)
    regbankcol(io.cmd.bits.rs1) := io.cmd.bits.rs2(5,0)
  }
//  when(set_j){
//    dimJ := cmd.bits.rs1
//  }
//  when(set_k) {
//    dimK := cmd.bits.rs1
//  }

  baseAddress := Mux(state === waiting_for_command, io.cmd.bits.rs1, 0.U)
  bank_num_0 := Mux(state === waiting_for_command, io.cmd.bits.inst.rs1, 0.U)
  bank_num := Mux(state === waiting_for_command, io.cmd.bits.inst.rs2, 0.U)
  res_bank := Mux(state === waiting_for_command, io.cmd.bits.inst.rd, 0.U)

  switch (state) {
    is(waiting_for_command) {
      io.cmd.ready := 1.U
      when(cmd.fire()) {
        when(load) {
          state := start_load
//          isA := true.B
//        }.elsewhen(load_B) {
//          state := start_load
//          isA := false.B
        }.elsewhen(calc) {
          state := compute
        }.elsewhen(store){
          state := store_data
        }
      }
    }
    is(start_load) {
      when(load_ctrl.io.cmd.fire()) {
        state := loading
      }
    }
    is(loading){
//      io.mem <> load_ctrl.io.memIO

      when(load_ctrl.io.complete){
        state := waiting_for_command
      }
    }
    is(compute){
      when(!(exec_ctrl.io.regbankreq.fire() || exec_ctrl.io.regbankreqTrans.fire() || systolic.io.out.fire())){
        state := waiting_for_command //TODO
      }
    }
    is(store_data) {
      when(str_ctrl.io.complete){
        state := waiting_for_command
      }
    }
  }


//  when (cmd.fire() && (doWrite || doAccum)) {
//    regfile(addr) := wdata
//  }
//
//  when (io.mem.resp.valid) {
//    regfile(memRespTag) := io.mem.resp.bits.data
//    busy(memRespTag) := false.B
//  }
//
//  // control
//  when (io.mem.req.fire()) {
//    busy(addr) := true.B
//  }
//
//  val doResp = cmd.bits.inst.xd
//  val stallReg = busy(addr)
//  val stallLoad = doLoad && !io.mem.req.ready
//  val stallResp = doResp && !io.resp.ready

  // command resolved if no stalls AND not issuing a load that will need a request

  // PROC RESPONSE INTERFACE
//  io.resp.valid := cmd.valid && doResp && !stallReg && !stallLoad
//  // valid response if valid command, need a response, and no stalls
//  io.resp.bits.rd := cmd.bits.inst.rd
//  // Must respond with the appropriate tag or undefined behavior
//  io.resp.bits.data := accum
//  // Semantics is to always send out prior accumulator register value
//
//  io.busy := state =/= waiting_for_command
//  // Be busy when have pending memory requests or committed possibility of pending requests
//  io.interrupt := false.B
  // Set this true to trigger an interrupt on the processor (please refer to supervisor documentation)

  // MEMORY REQUEST INTERFACE
//  io.mem.req.valid := cmd.valid && doLoad && !stallReg && !stallResp
//  io.mem.req.bits.addr := addend
//  io.mem.req.bits.tag := addr
//  io.mem.req.bits.cmd := M_XRD // perform a load (M_XWR for stores)
//  io.mem.req.bits.size := log2Ceil(8).U
//  io.mem.req.bits.signed := false.B
//  io.mem.req.bits.data := 0.U // we're not performing any stores...
//  io.mem.req.bits.phys := false.B
//  io.mem.req.bits.dprv := cmd.bits.status.dprv
  io.mem.keep_clock_enabled := true.B
  io.mem.s1_data.data := 0.U   // prefetcher writes nothing
  io.mem.s1_data.mask := 0.U
  io.mem.s1_kill := false.B  // prefetch kills nothing
  io.mem.s2_kill := false.B

}

class WithAccel extends Config ((site, here, up) => {
//  case Sha3WidthP => 64
//  case Sha3Stages => 1
//  case Sha3FastMem => true
//  case Sha3BufferSram => false
//  case Sha3Keccak => false
//  case Sha3BlackBox => false
//  case Sha3TLB => Some(TLBConfig(nSets = 1, nWays = 4, nSectors = 1, nSuperpageEntries = 1))
  case BuildRoCC => up(BuildRoCC) ++ Seq(
    (p: Parameters) => {
      val sha3 = LazyModule.apply(new Accelerator(OpcodeSet.custom2)(p))
      sha3
    }
  )
})