package ECE499

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.tile._
import freechips.rocketchip.rocket._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import systolic._


class Accelerator(opcodes: OpcodeSet, val n: Int = 4, val dwidth: Int=64)
                       (implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AcceleratorModule(this, n, dwidth)
}

class AcceleratorModule(outer: Accelerator, dim: Int, dwidth: Int)
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
  val num_reg = 2

  val waiting_for_command :: start_load :: loading :: compute :: start_store :: store_data :: Nil = Enum(6)
  val state = RegInit(waiting_for_command)

  cmd.ready := (state===waiting_for_command)
  val funct = cmd.bits.inst.funct
  val addr = cmd.bits.rs1(log2Up(outer.n)-1,0)
  val set = funct === 0.U && cmd.fire()
//  val set_j = funct === 1.U
//  val set_k = funct === 2.U
  val load = funct === 1.U && cmd.fire()
  val calc = funct === 2.U && cmd.fire()
  val store = funct === 3.U && cmd.fire()
  val memRespTag = io.mem.resp.bits.tag(log2Up(outer.n)-1,0)
  val dimI = RegInit(dim.U)
  val dimJ = RegInit(dim.U)
  val dimK = RegInit(dim.U)
//  val isA = RegInit(true.B)
  val baseAddress = RegInit(0.U(64.W))
  val bank_num_0 = RegInit(0.U(num_reg.W))
  val bank_num = RegInit(0.U(num_reg.W))
  val res_bank = RegInit(0.U(num_reg.W))

  val load_ctrl = Module(new LoadController(dim, dwidth, num_reg, 64.W))
  val exec_ctrl = Module(new ExecuteController(dim, dwidth, num_reg, 64.W))
  val str_ctrl = Module(new StoreController(dim, dwidth, 64.W))
  val reg_bank_seq = (for (i <- 0 until num_reg)  yield Module(new RegBank(dim, 64)))
  val reg_bank = VecInit(reg_bank_seq.map(_.io))
//  val out_reg = Module(new OutRegBank(dim, 32))
  val systolic = Module(new systolic_d(dim, dwidth))
  val regbankrow = RegInit(VecInit(Seq.fill(num_reg)(0.U(dim.W)))) // TODO: log2up(dim)
  val regbankcol = RegInit(VecInit(Seq.fill(num_reg)(0.U(dim.W))))
  val transpose_reg = Module(new TransposeReg(dim, dwidth))

  //load instructions
  load_ctrl.io.cmd.valid := (state===start_load)
  load_ctrl.io.cmd.bits.row := regbankrow(bank_num)
  load_ctrl.io.cmd.bits.col := regbankcol(bank_num)
  load_ctrl.io.cmd.bits.address := baseAddress

  //execute instruction
  exec_ctrl.io.startExec := (state===compute)
  for(i<- 0 until num_reg){
    reg_bank(i).sys.cmd.bits := exec_ctrl.io.regbankreqTrans.bits
    reg_bank(i).sys.cmd.valid := exec_ctrl.io.regbankreqTrans.valid
    reg_bank(i).mem.cmd.bits := Mux(state===loading, load_ctrl.io.regbank.bits, str_ctrl.io.regbank.cmd.bits)
    reg_bank(i).mem.cmd.valid := Mux(i.U===bank_num, load_ctrl.io.regbank.valid || str_ctrl.io.regbank.cmd.valid, false.B)

//    reg_bank(i).read.req <> Mux(i.U===bank_num, exec_ctrl.io.regbankreqTrans, exec_ctrl.io.regbankreq)
  }
  reg_bank(bank_num_0).sys.cmd.bits := exec_ctrl.io.regbankreq.bits
  reg_bank(bank_num_0).sys.cmd.valid := exec_ctrl.io.regbankreq.valid
  when(systolic.io.out.valid) {
    reg_bank(res_bank).sys.cmd.bits := systolic.io.out.bits
    reg_bank(res_bank).sys.cmd.valid := systolic.io.out.valid && state===compute
  }

  transpose_reg.io.load := reg_bank(bank_num).sys.out
  transpose_reg.io.sys.cmd := exec_ctrl.io.regbankreq
  systolic.io.in_A := reg_bank(bank_num_0).sys.out.bits
  systolic.io.in_B := transpose_reg.io.sys.out.bits
  systolic.io.calc := exec_ctrl.io.regbankreq.valid

//  out_reg.io.load <> systolic.io.out

  str_ctrl.io.cmd.valid := state === start_store
  str_ctrl.io.cmd.bits.col := regbankcol(bank_num)
  str_ctrl.io.cmd.bits.row := regbankrow(bank_num)
  str_ctrl.io.cmd.bits.address := baseAddress
  reg_bank(bank_num).mem.out <> str_ctrl.io.regbank.out
//  out_reg.io.read<>str_ctrl.io.outreg

  when(state === loading){
    io.mem.req.bits <> load_ctrl.io.memReq.bits
  }.otherwise {
    io.mem.req.bits <> str_ctrl.io.memReq.bits
  }
  io.mem.req.valid := load_ctrl.io.memReq.valid || str_ctrl.io.memReq.valid
  load_ctrl.io.memReq.ready := io.mem.req.ready && state === loading
  str_ctrl.io.memReq.ready := io.mem.req.ready && state === store_data

  load_ctrl.io.memResp <> io.mem.resp
  str_ctrl.io.memResp <> io.mem.resp


  cmd.ready := (state === waiting_for_command)
  when(set){
    regbankrow(cmd.bits.inst.rd) := cmd.bits.rs1(num_reg, 0)
    regbankcol(cmd.bits.inst.rd) := cmd.bits.rs2(num_reg, 0)
  }
//  when(set_j){
//    dimJ := cmd.bits.rs1
//  }
//  when(set_k) {
//    dimK := cmd.bits.rs1
//  }

  baseAddress := Mux(state === waiting_for_command, cmd.bits.rs1, baseAddress)
  bank_num_0 := Mux(state === waiting_for_command, cmd.bits.inst.rs1, bank_num_0)
  bank_num := Mux(state === waiting_for_command, cmd.bits.inst.rs2, bank_num)
  res_bank := Mux(state === waiting_for_command, cmd.bits.inst.rd, res_bank)
  switch (state) {
    is(waiting_for_command) {
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
          state := start_store
        }.otherwise{
          state := state
        }
      }
    }
    is(start_load) {
      state := Mux(load_ctrl.io.cmd.fire(), loading, state)
//      when(load_ctrl.io.cmd.fire()) {
//        state := loading
//      }
    }
    is(loading){
//      io.mem <> load_ctrl.io.memIO
      state := Mux(load_ctrl.io.complete, waiting_for_command, state)
//      when(load_ctrl.io.complete){
//        state := waiting_for_command
//      }
    }
    is(compute){
      state := Mux(exec_ctrl.io.complete, waiting_for_command, state)
//      when(!(exec_ctrl.io.regbankreq.fire() || exec_ctrl.io.regbankreqTrans.fire() || systolic.io.out.fire())){
//        state := waiting_for_command //TODO
//      }
    }
    is(start_store) {
      state := Mux(str_ctrl.io.cmd.fire(), store_data, state)
      //      when(load_ctrl.io.cmd.fire()) {
      //        state := loading
      //      }
    }
    is(store_data) {
      state := Mux(str_ctrl.io.complete, waiting_for_command, state)
//      when(str_ctrl.io.complete){
//        state := waiting_for_command
//      }
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
  io.busy := state =/= waiting_for_command || cmd.valid
//  // Be busy when have pending memory requests or committed possibility of pending requests
//  io.interrupt := false.B
  // Set this true to trigger an interrupt on the processor (please refer to supervisor documentation)

  // MEMORY REQUEST INTERFACE
//  io.mem.req.valid := cmd.valid && doLoad && !stallReg && !stallResp
//  io.mem.req.bits.addr := addend
//  io.mem.req.bits.tag := addr
//  io.mem.req.bits.cmd := M_XRD // perform a load (M_XWR for stores)
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