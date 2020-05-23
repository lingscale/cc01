package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

import Control._

class DatapathIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val ifu = new IcbIO
  val lsu = new IcbIO
  val ext_irq = Input(Bool())
  val sft_irq = Input(Bool())
  val tmr_irq = Input(Bool())
  val pre_ctrl = Flipped(new ControlSignals)  // pre decode
  val ctrl     = Flipped(new ControlSignals)  // post decode
}

class Datapath(implicit val p: Parameters) extends Module with CoreParams {
  val io      = IO(new DatapathIO)

  val alu     = Module(new ALU)
  val regFile = Module(new RegFile) 
  val csr     = Module(new CSR)
  val immGen  = Module(new ImmGen)
  val oitf    = Module(new Oitf)

  /***** Fetch Registers *****/
  val pc = RegInit(Const.PC_RTVEC.U(xlen.W))
  val misalign = RegInit(false.B)

  /***** Fetch / Execute Registers *****/
  val fe_inst = Reg(UInt(xlen.W))
  val fe_pc   = Reg(UInt(xlen.W))

  val fe_pc_vld = RegInit(false.B)

  val fe_misalign = RegInit(false.B)
  val fe_buserr = Reg(Bool())

  val fe_illegal = Reg(Bool())

  val fe_br_prdt = Reg(Bool())

  val fe_rs1idx = Reg(UInt(5.W))
  val fe_rs2idx = Reg(UInt(5.W))

  /* valid-ready signals between instructionfetch-decode-dispatch-alu(mem)-commit-writeback */
  val fe_valid = RegInit(false.B)
  val fe_ready = Wire(Bool())

  val da_valid = Wire(Bool())  // dispatch alu/csr
  val da_ready = Wire(Bool())

  val do_valid = Wire(Bool())  // dispatch oitf
  val do_ready = Wire(Bool())

  val ac_valid = Wire(Bool())  // alu commit
  val ac_ready = Wire(Bool())
    val ac_branchslv_valid = Wire(Bool())
    val ac_excp_valid = Wire(Bool())
    val ac_branchslv_ready = Wire(Bool())
    val ac_excp_ready = Wire(Bool())
  val aw_valid = Wire(Bool())  // alu write back
  val aw_ready = Wire(Bool())

  val al_valid = Wire(Bool())  // alu load store unit
  val al_ready = Wire(Bool())

  val lc_valid = Wire(Bool())  // lsu commit
  val lc_ready = Wire(Bool())
  val lw_valid = Wire(Bool())  // lsu write back
  val lw_ready = Wire(Bool())

  val lo_valid = Wire(Bool())  // lsu oitf
  val lo_ready = Wire(Bool())

  /* signals between ifu and commit */
  val pipe_flush_req = Wire(Bool())
  val pipe_flush_ack = Wire(Bool())
  val pipe_flush_hsked = pipe_flush_req & pipe_flush_ack
  val pipe_flush_add_op1 = Wire(UInt(xlen.W))
  val pipe_flush_add_op2 = Wire(UInt(xlen.W))
  val wfi_halt_ifu_req = Wire(Bool())
  val wfi_halt_ifu_ack = RegInit(false.B)

  /* signals between execute and commit */
  val wfi_halt_exu_req = Wire(Bool())
  val wfi_halt_exu_ack = Wire(Bool())

  val oitf_empty = Wire(Bool())


  //////////////////////////////////////////////////////////////////////////////
  // instruction fetch
  //////////////////////////////////////////////////////////////////////////////

  val dly_pipe_flush_req = RegInit(false.B)
  val bjp_req = Wire(Bool())
  val bjp_pc_add_op1 = Wire(UInt(xlen.W))
  val bjp_pc_add_op2 = Wire(UInt(xlen.W))

  // when ifu response invalid, the response data may change, for example, the lsu reads data from the same memory area
  // the ifu just accessed (e.g itcm), so need keep npc invariable to prevent ifu response data (inst) generate wrong
  // ifu access address (and its value was saved in pc by ifu.rsp.fire), that's why !io.ifu.rsp.valid exists here. in
  // fact, that's a kind of bubble, or pipeline stall.
  val npc_add_op1 = Mux(pipe_flush_req,     pipe_flush_add_op1,
                    Mux(dly_pipe_flush_req, pc,
                    Mux(!io.ifu.rsp.valid,  pc,
                    Mux(bjp_req,            bjp_pc_add_op1,
                                            pc))))

  val npc_add_op2 = Mux(pipe_flush_req,     pipe_flush_add_op2,
                    Mux(dly_pipe_flush_req, 0.U,
                    Mux(!io.ifu.rsp.valid,  0.U,
                    Mux(bjp_req,            bjp_pc_add_op2,
                                            4.U))))

  val npc = npc_add_op1 + npc_add_op2

  val inst = io.ifu.rsp.bits.rdata

  // When to fetch instruction? There's demand, and last request has finished:
  //   1. not waiting & not halting, or flush request or flush request pending, then continues fetching, and
  //   2. last instruction fetch response has returned, or being returning, marked by ifu_out_flag
  val ifu_out_flag = RegInit(false.B)
  when (io.ifu.rsp.fire) { ifu_out_flag := false.B }
  when (io.ifu.cmd.fire) { ifu_out_flag := true.B }

  val jalr_wait = Wire(Bool())
  val pipe_flush_req_real = Wire(Bool())
  io.ifu.cmd.valid := ((!jalr_wait && !wfi_halt_ifu_req) || pipe_flush_req_real) && (!ifu_out_flag || io.ifu.rsp.fire)
  io.ifu.cmd.bits.addr := npc
  io.ifu.cmd.bits.read := true.B
  io.ifu.cmd.bits.wdata := DontCare
  io.ifu.cmd.bits.wmask := DontCare

  // When to update pc?
  //   1. the instruction fetch request being accepted or
  //   2. pipe flush can't be accepted (ifu.rsp not ready, back pressure ifu.cmd ready, new ifu.cmd requst can't be
  //      accepted), remember the flush pc temporary, to be used by dly_pipe_flush_req or
  //   3. when ifu.rsp.fire, to keep npc temporary, in case next ifu.rsp.ready invalid.
  when (io.ifu.cmd.fire || pipe_flush_hsked || io.ifu.rsp.fire) { pc := npc }

  when (io.ifu.cmd.fire) { when (npc(1, 0).orR) { misalign := true.B } .otherwise { misalign := false.B } }

  // When is ready to accept instruction? Instruction register is ready:
  //   1. fe_inst is empty, or bing clearing, and not jalr_wait, or
  //   2. pipe flush, which means when flush, just read current returned instruction and
  //      throw it (fe_valid deasserted by pipe_flush_hasked, and when pipe_flush_req_real, fe_valid won't
  //      be asserted, and fe_inst won't be update too), to prepare to accept flush address instruction.
  // Note back pressure here:
  //   1. the following stage (execute stage) hasn't finished process last instruction (marked by fe_ready
  //      desasseted), do not accept new instruction
  //   2. ifu not ready to accept, do not accept. for example when access qspi flash etc. by BIU (bus
  //      interface unit), need multiple cycles to return instruction, i.e. control instruction fetch rythm by
  //      valid-ready mechanism (rsp ready back press cmd ready).
  io.ifu.rsp.ready := ((!fe_valid || (fe_valid && fe_ready)) && !jalr_wait) || pipe_flush_req_real

  // When to update fe_inst? New instruction fetched and no pipe flush. Note valid flag fe_valid.
  when (io.ifu.rsp.fire && !pipe_flush_req_real) { fe_inst := io.ifu.rsp.bits.rdata }
  when (io.ifu.rsp.fire && !pipe_flush_req_real) { fe_buserr := io.ifu.rsp.bits.err }
  when (io.ifu.rsp.fire && !pipe_flush_req_real) { fe_illegal := io.pre_ctrl.illegal }


  // When new instruction fetched and no pipe flush, fe_valid will be set,
  // when the instruction is accepted by execute stage, or pipe flush, fe_valid will
  // be clear. And set has higher priority.
  when ((fe_valid && fe_ready) || (fe_valid && pipe_flush_hsked)) { fe_valid := false.B }  // there may without && fe_valid, but leave it to synthesize tools
  when (io.ifu.rsp.fire && !pipe_flush_req_real) { fe_valid := true.B }

  // pc_newpending means a new effective pc loaded (io.ifu.cmd.fire || pipe_flush_hsked), waiting be loaded to fe_pc
  val pc_newpending = RegInit(false.B)
  when (pc_newpending && (!fe_valid || (fe_valid && fe_ready)) && !pipe_flush_req_real) { pc_newpending := false.B }
  when (io.ifu.cmd.fire || pipe_flush_hsked) { pc_newpending := true.B }

  // When to update fe_pc? effective pc updated and fe_inst ready to accept new instruction, and
  // no pipe flush. Note valid flag.
  when (pc_newpending && (!fe_valid || (fe_valid && fe_ready)) && !pipe_flush_req_real) { fe_pc := pc }
  when (pc_newpending && (!fe_valid || (fe_valid && fe_ready)) && !pipe_flush_req_real) { fe_misalign := misalign }

  // fe_pc_vld records if fe_pc according with fe_insr, used when exception happens, as mepc
  // fe_pc_vld is used by the commit stage to check if current instruction is outputing a valid current PC
  // to guarante the commit to flush pipeline safely, this vld only be asserted whenthere is a valid instruction here,
  // otherwise, the commit stage may use wrong PC value to stored in mepc.
  // Because asynochronous precise exception (interupt exception) need to use the next upcoming (not yet commited, i.e.
  //   fe_valid not asserted) instruction's PC for the mepc value, so we must wait next valid instruction coming and use
  //   its PC. The fe_pc_vld is just used to indicate next instruction's valid PC value.
  when ((fe_valid && fe_ready) || (fe_valid && pipe_flush_hsked)) { fe_pc_vld := false.B }  // there must && fe_valid
  when (pc_newpending && (!fe_valid || (fe_valid && fe_ready)) && !pipe_flush_req_real) { fe_pc_vld := true.B }


  //////////////////////////////////////////////////////////////////////////////
  // jump and simple static branch prediction
  // JAL target current pc + offset
  // JALR with rs1 = x0, target  0 + offset
  // JALR with rs1 = x1, target x1 + offxet
  //   x1 value is hardwired from register file.
  //   x1 dependency check:
  //     1. oitf in exu is empty (just for simplicity, oitf not empty doesn't mean
  //        there must be dependency)
  //     2. fe_inst doesn't use x1 as destination register
  //     if dependency here, assert jalr_wait to hold up ifu until dependency clear
  // JALR with rs1 =/= x0 or x1, target xn + offxet
  //   target address needs be resolved at exu stage, must halt and wait exu clear
  //   then read xn. this will exert 1 cycle performance lost.
  //
  // Bxxx target current pc + offset
  //   predicted as taken if it is backward jump, not taken the contrast.

  val jalr_rs1x1_hazard = fe_valid && io.ctrl.need_rd && (fe_inst(11, 7) === 1.U)
  val jalr_rs1x1_dep = io.ifu.rsp.valid && io.pre_ctrl.jalr && (inst(19, 15) === 1.U) && (!oitf_empty || jalr_rs1x1_hazard)

  val jalr_rs1xn_dep = io.ifu.rsp.valid && io.pre_ctrl.jalr && inst(19, 16).orR && (!oitf_empty || fe_valid)
  // If only depend to IR stage (OITF is empty), and fe_inst is under clearing, or
  // it does not use RS1 index, then also treated as non-dependency.
  val jalr_rs1xn_dep_fe_inst_clr = jalr_rs1xn_dep && oitf_empty && ((fe_valid && fe_ready) || !io.ctrl.need_rs1)

  val jalr_rs1en = Wire(Bool())
  val rs1xn_read_rf = Reg(Bool())
  when (rs1xn_read_rf) { rs1xn_read_rf := false.B }
  when (jalr_rs1en) { rs1xn_read_rf := true.B }

  jalr_rs1en := !rs1xn_read_rf && io.ifu.rsp.valid && io.pre_ctrl.jalr && inst(19, 16).orR && (!jalr_rs1xn_dep || jalr_rs1xn_dep_fe_inst_clr)
  // When need read rs1 of register files, ifu should halt to prevent next pc's generation.
  jalr_wait := jalr_rs1x1_dep || jalr_rs1xn_dep || jalr_rs1en

  val br_prdt = io.pre_ctrl.br && inst(31).asBool

  bjp_req := io.pre_ctrl.jal || io.pre_ctrl.jalr || br_prdt

  bjp_pc_add_op1 := Mux(io.pre_ctrl.br,                               pc,
                    Mux(io.pre_ctrl.jal,                              pc,
                    Mux((io.pre_ctrl.jalr && (inst(19, 15) === 0.U)), 0.U(32.W),
                    Mux((io.pre_ctrl.jalr && (inst(19, 15) === 1.U)), regFile.io.x1,
                                                                      regFile.io.rs1))))

  bjp_pc_add_op2 := Mux(io.pre_ctrl.br,                               Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)),
                    Mux(io.pre_ctrl.jal,                              Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)),
                    Mux((io.pre_ctrl.jalr && (inst(19, 15) === 0.U)), Cat(Fill(20, inst(31)), inst(31, 20)),
                    Mux((io.pre_ctrl.jalr && (inst(19, 15) === 1.U)), Cat(Fill(20, inst(31)), inst(31, 20)),
                                                                      Cat(Fill(20, inst(31)), inst(31, 20))))))

  fe_br_prdt := br_prdt


  ///////////////////////////////////////////////////////////////////////////
  // halt acknowledge generation, for wfi
  // instruction fetch unit will ackonwledge halt request when:
  //   halt_req asserted, halt_ack not asserted, instruction fetch request channel
  //   is ready, i.e. no outstanding transaction
  // Note here uses io.ifu.rsp.valid, not io.ifu.rsp.fire, 'cause wfi prevent the excution
  //   of then next following instruction, which hence can't be accepted by fe_inst, or means bug.
  //   And io.ifu.rsp.valid means last instruction fetch request have response back, meanwhile
  //   halt_req prevents new fetch request, means no fetch outstanding.
  //   (and remember, ifu just only have one outstanding, so io.ifu.rsp.valid means no out)
  when (wfi_halt_ifu_ack && !wfi_halt_ifu_req) { wfi_halt_ifu_ack := false.B }
  when (wfi_halt_ifu_req && !wfi_halt_ifu_ack && (!ifu_out_flag || io.ifu.rsp.valid)) { wfi_halt_ifu_ack := true.B }
  

  ///////////////////////////////////////////////////////////////////////////
  // pipe flush acknowledge generation, for branchmis, exp, irq
  // Ideally the flush is acknowledged when instruction fetch unit is ready, or there
  //   is response valid.
  // But to cut the comb loop between execute unit and insturction fetch unit, flush
  //   request always be accepted, and use delayed flush indication to remember this
  //   flush. Note even a delayed flush pending here, still accept new flush request,
  //   old flush request just be flushed. This may be happen when interrupt nesting or
  //   something, which will be dealt by software.
  // What does comb loop mean? exu waiting for ifu to accept flush request, and ifu
  //   wait exu to finish exuction last insturction -- which generates flush request,
  //   then fe_inst can't be clear, and ifu req can't fire, and can't accept flush
  //   request, so deadlock.
  pipe_flush_ack := true.B

  when (io.ifu.cmd.fire) { dly_pipe_flush_req := false.B }
  when (pipe_flush_req && !io.ifu.cmd.fire) { dly_pipe_flush_req := true.B }

  pipe_flush_req_real := pipe_flush_req || dly_pipe_flush_req


  ////////////////////////////////////////////////////////////////////////////////////////////
  // rs1 index and rs2 index prepared at instruction fetch stage, and only update when needed
  // index equal zero is also updated, this may consume some power, but can simplity following
  // stage logic, and may save power consumation there.
  when (io.ifu.rsp.fire && !pipe_flush_req_real && io.pre_ctrl.need_rs1 || jalr_rs1en) { fe_rs1idx := inst(19, 15) }
  when (io.ifu.rsp.fire && !pipe_flush_req_real && io.pre_ctrl.need_rs2) { fe_rs2idx := inst(24, 20) }


  // pre decode, belongs instruction fetch stage
  io.pre_ctrl.inst := inst


  //////////////////////////////////////////////////////////////////////////////
  // decode
  //////////////////////////////////////////////////////////////////////////////

  // post decode, belongs execute stage
  io.ctrl.inst := fe_inst


  //////////////////////////////////////////////////////////////////////////////
  // dispatch
  //////////////////////////////////////////////////////////////////////////////

  // The Dispatch Scheme Introduction for two-pipeline stage
  //  1st: The instruction dispatched must have already had operand fetched, so
  //      there is no WAR dependency happened.
  //  2nd: The ALU-instructions are dispatched and executed in-order inside ALU, so
  //      there is no any WAW dependency happened among ALU instructions.
  //  3rd: The non-ALU-instruction (load, store) are all tracked by OITF, and must be write-back in-order, so
  //      it is like ALU in-ordered. So there is no any WAW dependency happened among
  //      non-ALU instructions.
  //  Then what dependency will we have?
  //  * RAW: This is the real dependency
  //  * WAW: The WAW between ALU an non-ALU instructions
  //
  //  So 1st, The dispatching ALU instruction can not proceed and must be stalled when
  //      ** RAW: The ALU reading operands have data dependency with OITF entries
  //         *** Note: since it is 2 pipeline stage, any last ALU instruction have already
  //             write-back into the regfile. So there is no chance for ALU instr to depend
  //             on last ALU instructions as RAW.
  //      ** WAW: The ALU writing result have no any data dependency with OITF entries
  //  And 2nd, The dispatching non-ALU instruction can not proceed and must be stalled when
  //      ** RAW: The non-ALU reading operands have data dependency with OITF entries
  //         *** Note: since it is 2 pipeline stage, any last ALU instruction have already
  //             write-back into the regfile. So there is no chance for non-ALU instr to depend
  //             on last ALU instructions as RAW.

  val raw_dep = oitf.io.oitfrd_match_disrs1 || oitf.io.oitfrd_match_disrs2

  // Only check the longp instructions (non-ALU) for WAW, here if we
  //   use the precise version (disp_alu_longp_real), it will hurt timing very much, but
  //   if we use imprecise version of disp_alu_longp_prdt, it is kind of tricky and in
  //   some corner case. For example, the AGU ( treated as longp ) will actually not dispatch /* unaligned load store */
  //   to longp but just directly commited, then it become a normal ALU instruction, and should
  //   check the WAW dependency, but this only happened when it is AMO or unaligned-uop, so
  //   ideally we dont need to worry about it, because
  //     * We dont support AMO in 2 stage CPU here
  //     * We dont support Unalign load-store in 2 stage CPU here, which
  //         will be triggered as exception, so will not really write-back
  //         into regfile
  // Nevertheless: using this condition only waiver the longpipe WAW case, that is, two
  //   longp instruction write-back same reg back2back. Is it possible or is it common? /* no reason to do so */
  //   after we checking the benmark result we found if we remove this complexity here
  //   it just does not change any benchmark number, so just remove that condition out. Means
  //   all of the instructions will check waw_dep
  //wire alu_waw_dep = ( ~disp_alu_longp_prdt ) & ( oitfrd_match_disprd & disp_i_rdwen );

  val waw_dep = oitf.io.oitfrd_match_disrd

  val dep = raw_dep | waw_dep;

  val disp_condition = Mux(io.ctrl.csr, oitf_empty, true.B) &&  // To be more conservative, any accessing CSR instruction need to wait the oitf to be empty. e.g. lsu excp happens
                       Mux(io.ctrl.fence || io.ctrl.fencei, oitf_empty, true.B) &&  // To handle the Fence: just stall dispatch until the OITF is empty
                       !wfi_halt_exu_req &&  // If it was a WFI instruction commited halt req, then it will stall the disaptch
                       !dep &&  // No dependency
                       Mux(io.ctrl.ld || io.ctrl.st, oitf.io.dis_ready, true.B)
                       // unaligned load store treated as exception, need no disp_oitf. when it happens, a trap service start, which spend
                       // much more time than this disp oitf ready waiting, and unalinged ld st aren't encouraged. so for timing considition and
                       // simplicity, just check oitf ready for all ld st instructions

  da_valid := disp_condition && fe_valid  // dispatch alu/csr

  val ls_align = Wire(Bool())
  do_valid := ls_align && da_valid && da_ready
  oitf.io.dis_valid := do_valid
  do_ready := oitf.io.dis_ready

  oitf.io.dis_rs1en  := io.ctrl.need_rs1 && fe_inst(19, 15).orR
  oitf.io.dis_rs2en  := io.ctrl.need_rs2 && fe_inst(24, 20).orR
  oitf.io.dis_rdwen  := io.ctrl.need_rd  && fe_inst(11,  7).orR
  oitf.io.dis_rs1idx := fe_rs1idx
  oitf.io.dis_rs2idx := fe_rs2idx
  oitf.io.dis_rdidx  := fe_inst(11,  7)
  oitf.io.dis_pc     := fe_pc

  oitf_empty := oitf.io.empty

  wfi_halt_exu_ack := oitf_empty;  // The WFI halt exu ack will be asserted when the OITF is empty

  fe_ready := disp_condition && da_ready


  //////////////////////////////////////////////////////////////////////////////
  // execute/alu(mem)
  //////////////////////////////////////////////////////////////////////////////

  regFile.io.rs1idx := fe_rs1idx
  regFile.io.rs2idx := fe_rs2idx

  immGen.io.inst := fe_inst
  immGen.io.sel := io.ctrl.imm_sel

  alu.io.A := Mux(io.ctrl.A_sel === A_RS1, regFile.io.rs1, fe_pc)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, regFile.io.rs2, immGen.io.out)
  alu.io.alu_op := io.ctrl.alu_op
  alu.io.br_type := io.ctrl.br_type

  csr.io.rs1 := regFile.io.rs1
  csr.io.inst := fe_inst
  csr.io.cmd := io.ctrl.csr_cmd

  val alu_wdat = Mux(io.ctrl.csr_cmd === CSR.N, alu.io.out, csr.io.out)

  val ls_misalign = ((io.ctrl.ld_type === LD_LW) && alu.io.out(1, 0).orR) ||
                    ((io.ctrl.ld_type === LD_LH) && alu.io.out(0)) ||
                    ((io.ctrl.ld_type === LD_LHU) && alu.io.out(0)) ||
                    ((io.ctrl.st_type === ST_SW) && alu.io.out(1, 0).orR) ||
                    ((io.ctrl.st_type === ST_SH) && alu.io.out(0))

  val lsu_need_flush = Wire(Bool())
  val intrp_req = Wire(Bool())
  ls_align := (io.ctrl.ld || io.ctrl.st) && !ls_misalign && !lsu_need_flush && !intrp_req

  val alu_need_cmit = true.B
  val alu_need_wbck = io.ctrl.need_rd && !io.ctrl.ld && fe_inst(11, 7).orR && !(fe_misalign || fe_buserr || fe_illegal)
                                                        /* rdidx =/= 0 */
  ac_valid := alu_need_cmit && da_valid && Mux(ls_align, al_ready, true.B) && Mux(alu_need_wbck, aw_ready, true.B)
  aw_valid := alu_need_wbck && da_valid && Mux(alu_need_cmit, ac_ready, true.B)

  val acaw_ready = Mux(alu_need_cmit, ac_ready, true.B) && Mux(alu_need_wbck, aw_ready, true.B)
  al_valid := ls_align && da_valid && acaw_ready
  

  class lsinfo extends Bundle {
    val addr    = chiselTypeOf(alu.io.out)
    val ld_type = chiselTypeOf(io.ctrl.ld_type)
  }

  val lsinfo_queue = Module(new Queue(new lsinfo, entries = 1, pipe = true, flow = false))

  lsinfo_queue.io.enq.valid := io.lsu.cmd.fire
  lsinfo_queue.io.enq.bits.addr := alu.io.out
  lsinfo_queue.io.enq.bits.ld_type := io.ctrl.ld_type

  lsinfo_queue.io.deq.ready := io.lsu.rsp.fire

  io.lsu.cmd.valid := al_valid && lsinfo_queue.io.enq.ready
  io.lsu.cmd.bits.addr := alu.io.out
  io.lsu.cmd.bits.read := io.ctrl.ld
  io.lsu.cmd.bits.wdata := regFile.io.rs2 << (alu.io.out(1) << 4.U | alu.io.out(0) << 3.U)
  io.lsu.cmd.bits.wmask := Mux(io.ctrl.st_type === ST_SW, "b1111".U(4.W),
                           Mux(io.ctrl.st_type === ST_SH, "b0011".U(4.W) << alu.io.out(1, 0).asUInt,
                           Mux(io.ctrl.st_type === ST_SB, "b0001".U(4.W) << alu.io.out(1, 0).asUInt, "b0000".U(4.W))))

  al_ready := io.lsu.cmd.ready && lsinfo_queue.io.enq.ready

  val lsu_need_cmit = io.lsu.rsp.bits.err
  val lsu_need_wbck = oitf.io.ret_rdwen && !io.lsu.rsp.bits.err

  val lsu_wdat_raw = io.lsu.rsp.bits.rdata >> (lsinfo_queue.io.deq.bits.addr(1) << 4.U | lsinfo_queue.io.deq.bits.addr(0) << 3.U)
  val lsu_wdat = MuxLookup(lsinfo_queue.io.deq.bits.ld_type, lsu_wdat_raw.asSInt, Seq(
                   LD_LH  -> lsu_wdat_raw(15, 0).asSInt, LD_LB  ->lsu_wdat_raw(7, 0).asSInt,
                   LD_LHU -> lsu_wdat_raw(15, 0).zext,   LD_LBU -> lsu_wdat_raw(7, 0).zext)).asUInt

  val lclw_ready = Mux(lsu_need_cmit, lc_ready, true.B) && Mux(lsu_need_wbck, lw_ready, true.B)

  io.lsu.rsp.ready := lo_ready && lclw_ready


  // theoretically lsu commit and writeback should synchronous, so both lc_ready and lw_ready should be true.
  // but in fact, lsu commit and wirteback nerver hanppes simulataneously, so no necessary to check lc_ready lw_ready.
  //   val lclw_ready = Mux(lsu_need_cmit, lc_ready, true.B) && Mux(lsu_need_wbck, lw_ready, true.B)
  // and more important, if check lc_ready lw_ready redundantly, signal ac_ready recursive definiton occurs.
  //  lc_valid := lsu_need_cmit && !oitf_empty && io.lsu.rsp.valid && Mux(lsu_need_wbck, lw_ready, true.B)
  //  lw_valid := lsu_need_wbck && !oitf_empty && io.lsu.rsp.valid && Mux(lsu_need_cmit, lc_ready, true.B)
  lc_valid := lsu_need_cmit && !oitf_empty && io.lsu.rsp.valid
  lw_valid := lsu_need_wbck && !oitf_empty && io.lsu.rsp.valid

  lo_valid := io.lsu.rsp.fire  // lsu to oitf
  oitf.io.ret_valid := lo_valid
  lo_ready := oitf.io.ret_ready

  da_ready := acaw_ready && Mux(ls_align, al_ready, true.B)


  //////////////////////////////////////////////////////////////////////////////
  // commit branch/exception
  //////////////////////////////////////////////////////////////////////////////
  val branch_flush_req = Wire(Bool())
  val excp_flush_req = Wire(Bool())
  pipe_flush_req := branch_flush_req || excp_flush_req

  val excp_flush_add_op1 = Wire(UInt(xlen.W))
  val excp_flush_add_op2 = Wire(UInt(xlen.W))
  val branch_flush_add_op1 = Wire(UInt(xlen.W))
  val branch_flush_add_op2 = Wire(UInt(xlen.W))
  pipe_flush_add_op1 := Mux(excp_flush_req, excp_flush_add_op1, branch_flush_add_op1)
  pipe_flush_add_op2 := Mux(excp_flush_req, excp_flush_add_op2, branch_flush_add_op2)

  ac_branchslv_valid := ac_valid
  ac_excp_valid := ac_valid

  val branch_flush_ack = pipe_flush_ack
  val excp_flush_ack = pipe_flush_ack

  //////////////////////////////////////////////////////////////////////////////
  // branch resolve
  // Branch predicted at instruction fetch stage, and resolved at execute stage, if mispredicted, need flush.
  // Mret always flush the next prefetched instruction.
  // Fencei also always flush the pipeline to ensure its effort.
  // Note Non-ALU flush (aligned lsu err flush, interrupt flush) will override the ALU flush.
  val branch_need_flush = (io.ctrl.br && (fe_br_prdt ^ alu.io.br_rslv)) || io.ctrl.mret || io.ctrl.fencei

  branch_flush_add_op1 := Mux(io.ctrl.mret,   csr.io.mepc,
                          Mux(io.ctrl.fencei, fe_pc,
                          Mux(fe_br_prdt,     fe_pc,
                                              fe_pc)))

  branch_flush_add_op2 := Mux(io.ctrl.mret,   0.U,
                          Mux(io.ctrl.fencei, 4.U,
                          Mux(fe_br_prdt,     4.U,
                                              immGen.io.out)))

  branch_flush_req := ac_branchslv_valid && branch_need_flush && !lsu_need_flush && !intrp_req  /* when lsu_need_flush (lsu buserr) or irq_req, alu branchmis should wait */

  csr.io.ret := io.ctrl.mret && branch_flush_req && branch_flush_ack

  csr.io.instret := ac_valid && ac_ready && !branch_flush_req

  ac_branchslv_ready := !(io.ctrl.br || io.ctrl.mret || io.ctrl.fencei) ||
                        (Mux(branch_need_flush, branch_flush_ack, true.B) && !lsu_need_flush && !intrp_req)

  //////////////////////////////////////////////////////////////////////////////
  // exception resolve

  // wfi
  val wfi_halt_req = RegInit(false.B)
  val intrp_req_raw = Wire(Bool())
  when (io.ctrl.wfi && ac_valid && ac_ready) { wfi_halt_req := true.B }
  when (intrp_req_raw) { wfi_halt_req := false.B }

  // make sure the flush to IFU and halt to IFU not asserted at the same cycle
  wfi_halt_ifu_req := wfi_halt_req && !intrp_req_raw;
  // To cut the comb loops, we dont use the clr signal here to qualify,
  //   the outcome is the halt-to-exu will be deasserted 1 cycle later than to-IFU
  //   but it doesnt matter much.
  wfi_halt_exu_req := wfi_halt_req;

  // there are three kinds of exceptions, priority top down
  //   lsu aligned access error exception
  //      asynochronous non-precise exception /* and fe_pc_vld? */
  //   interrupt exception
  //     should wait oitf empty and pc_vald asserted
  //     as it is asynochronous precise exception, needs the next following (not yet commited) instruction's pc
  //     for mepc, so should wait fe_pc_vld asserted, which just indicates next instruction's pc value valid.
  //   alu exception, should wait oitf empty
  lsu_need_flush := lc_valid  // lsu commit always ask for exception (lsu buserr, aligned memory access error)
  intrp_req_raw := (io.ext_irq && csr.io.MEIE) || (io.sft_irq && csr.io.MSIE) || (io.tmr_irq && csr.io.MTIE)
  intrp_req := intrp_req_raw && csr.io.MIE
  val alu_need_flush = fe_misalign ||     // instruction unalign, synchronous exception (exception cause 0)
                       fe_buserr ||       // instruction fetch response error, synchronous exception (exception cause 1)
                       fe_illegal ||      // illegal instruction, synchronous exception (exception cause 2)
                       ls_misalign ||     // unalign load/store, synchronous exception (exception cause 4, 6)
                       io.ctrl.ebreak ||  // used by debugger, synchronous exception (exception cause 3)
                       io.ctrl.ecall      // usually for operating system, synchronous exception (exception cause 11)

  val lsu_excp_flush_req = lsu_need_flush   // Exclude the pc_vld for lsu, to just always make sure can always be accepted
  val intrp_excp_flush_req = intrp_req && oitf_empty && fe_pc_vld && !lsu_need_flush
  val alu_excp_flush_req = ac_excp_valid && alu_need_flush && oitf_empty && !intrp_req && !lsu_need_flush

  excp_flush_req := lsu_excp_flush_req || intrp_excp_flush_req || alu_excp_flush_req
  excp_flush_add_op1 := csr.io.mtvec
  excp_flush_add_op2 := 0.U(xlen.W)

  val narrow_intrp_flush_req = intrp_excp_flush_req
  val narrow_excp_flush_req  = lsu_excp_flush_req || alu_excp_flush_req

  val narrow_intrp_en = narrow_intrp_flush_req && excp_flush_req && excp_flush_ack
  val narrow_excp_en  = narrow_excp_flush_req  && excp_flush_req && excp_flush_ack

  val intrp_cause = Cat(1.U(1.W), 0.U(26.W), Mux(io.ext_irq && csr.io.MEIE, Cause.McnExtInpt,
                                             Mux(io.sft_irq && csr.io.MSIE, Cause.McnSftInpt,
                                             Mux(io.tmr_irq && csr.io.MTIE, Cause.McnTmrInpt, Cause.Reserved))))

  val excp_cause  = Cat(0.U(1.W), 0.U(26.W), Mux(alu_excp_flush_req && fe_misalign, Cause.InstAddrMisaligned,
                                             Mux(alu_excp_flush_req && fe_buserr, Cause.InstAccessFault,
                                             Mux(alu_excp_flush_req && fe_illegal, Cause.IllegalInst,
                                             Mux(alu_excp_flush_req && io.ctrl.ebreak, Cause.Breakpoint,
                                             Mux(alu_excp_flush_req && ls_misalign && io.ctrl.ld, Cause.LoadAddrMisaligned,
                                             Mux(lsu_excp_flush_req && lsinfo_queue.io.deq.bits.ld_type.orR, Cause.LoadAccessFault,
                                             Mux(alu_excp_flush_req && ls_misalign && io.ctrl.st, Cause.StoreAddrMisaligned,
                                             Mux(lsu_excp_flush_req && !lsinfo_queue.io.deq.bits.ld_type.orR, Cause.StoreAccessFault,
                                             Mux(alu_excp_flush_req && io.ctrl.ecall, Cause.EcallMMode, Cause.Reserved))))))))))

  csr.io.ext_irq := io.ext_irq
  csr.io.sft_irq := io.sft_irq
  csr.io.tmr_irq := io.tmr_irq

  csr.io.excp := narrow_excp_en || narrow_intrp_en

  csr.io.cause := Mux(narrow_intrp_en, intrp_cause, excp_cause)

  csr.io.errpc := Mux(lc_valid, oitf.io.ret_pc, fe_pc)  // used to update mepc

  csr.io.trapvalue := Mux(lsu_excp_flush_req, lsinfo_queue.io.deq.bits.addr,
                      Mux(ls_misalign, alu.io.out,
                      Mux(io.ctrl.ebreak, fe_pc,
                      Mux(fe_misalign, fe_pc,
                      Mux(fe_buserr, fe_pc,
                      Mux(fe_illegal, fe_inst, 0.U(xlen.W)))))))

  ac_excp_ready := Mux(alu_need_flush, excp_flush_ack && oitf_empty && !intrp_req && !lsu_need_flush, !intrp_req && !lsu_need_flush)

  ac_ready := ac_branchslv_ready && ac_excp_ready
  lc_ready := excp_flush_ack


  //////////////////////////////////////////////////////////////////////////////
  // write back
  //////////////////////////////////////////////////////////////////////////////
  // alu write back has higher priority
  // not every alu instruction needs write back, ld st instruction can write in the gap or just wait the gap
  // if depend happens, just need one extra clock. but if lsu has higher priority, it always waste one clock.
  // but unfortunately, otif can't enq when deqing, or get combinational loop, so load store instrcutions can't dispatch back by back, the more worse time waste. and how to solve?
  // add oitf depth to 2 can sove, but the simulation result seems alu write back has higher priority spend mor time.
/*  regFile.io.rdwen := Mux(lw_ready, lw_valid, aw_valid)
  regFile.io.rd    := Mux(lw_ready, lsu_wdat, alu_wdat)
  regFile.io.rdidx := Mux(lw_ready, oitf.io.ret_rdidx, fe_inst(11, 7))

  aw_ready := true.B
  lw_ready := !aw_valid
*/

  regFile.io.rdwen := Mux(aw_ready, aw_valid, lw_valid)
  regFile.io.rd    := Mux(aw_ready, alu_wdat, lsu_wdat)
  regFile.io.rdidx := Mux(aw_ready, fe_inst(11, 7), oitf.io.ret_rdidx)

  aw_ready := !lw_valid
  lw_ready := true.B

}



