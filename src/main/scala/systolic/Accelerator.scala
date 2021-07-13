package systolic

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.rocket.M_XRD
import freechips.rocketchip.tile._

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

  val rst :: waiting_for_command :: start_load :: loading :: transpose :: compute :: store_data :: Nil = Enum(6)
  val state = RegInit(waiting_for_command)

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

  val load_ctrl = new LoadController(dim, 64, num_reg, 64.W)
  val exec_ctrl = new ExecuteController(dim, 64, num_reg, 64.W)
  val str_ctrl = new StoreController(dim, 64, 64.W)
  val reg_bank = Vec(num_reg, Module(new RegBank(dim, 64)).io)
  val out_reg = new OutRegBank(dim, 64)
  val systolic = new systolic_d(dim, 64)
  val regbankrow = RegInit(Vec(num_reg, UInt(dim.W)))
  val regbankcol = RegInit(Vec(num_reg, UInt(dim.W)))
  val transpose_reg = new TransposeReg(dim, 64)

  load_ctrl.io.cmd.valid := state === start_load
  load_ctrl.io.cmd.bits.row := regbankrow(bank_num_0)
  load_ctrl.io.cmd.bits.col := regbankcol(bank_num_0)
//  load_ctrl.io.cmd.bits.isA := isA
  load_ctrl.io.cmd.bits.address := baseAddress

  exec_ctrl.io.startExec := state === compute
  when(exec_ctrl.io.regbankreqTrans.fire()){
    reg_bank(bank_num).read.req <> exec_ctrl.io.regbankreqTrans
    transpose_reg.io.load <> reg_bank(bank_num).read.resp
  }
  reg_bank(bank_num_0).read.req <> exec_ctrl.io.regbankreq
  transpose_reg.io.read.req <> exec_ctrl.io.regbankreq
  systolic.io.in_A <> reg_bank(bank_num_0).read.resp
  systolic.io.in_B <> transpose_reg.io.read.resp
  systolic.io.calc := exec_ctrl.io.regbankreq.valid

  out_reg.io.load <> systolic.io.out

  str_ctrl.io.cmd.valid := state === store_data
  str_ctrl.io.cmd.bits.col := dimJ
  str_ctrl.io.cmd.bits.row := dimI
  str_ctrl.io.cmd.bits.address := baseAddress
  out_reg.io.read<>str_ctrl.io.outreg


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

  switch (state) {
    is(waiting_for_command) {
      io.cmd.ready := 1.U
      when(cmd.fire()) {
        baseAddress := io.cmd.bits.rs1
        bank_num_0 := io.cmd.bits.inst.rs1
        bank_num := io.cmd.bits.inst.rs2
        res_bank := io.cmd.bits.inst.rd
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
      io.mem <> load_ctrl.io.memIO
      reg_bank(bank_num_0).load <> load_ctrl.io.regbank
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
      io.mem <> out_reg.mem
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

}