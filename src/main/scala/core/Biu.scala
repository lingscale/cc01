package lingscale.cc01.core

import chisel3._
import chisel3.util._
import scala.math.max
import scala.collection.mutable.ArraySeq
import lingscale.cc01.config.{Field, Parameters}

class BiuIO(implicit p: Parameters) extends Bundle {
  val lsu = Flipped(new IcbIO)  // load store unit
  val ifu = Flipped(new IcbIO)  // instruction fetch unit
  val ppi  = new IcbIO  // private perpheral interface
  val clic = new IcbIO  // core local interrupt controller
  val plic = new IcbIO  // platform-level interrupt controller
  val mem  = new IcbIO
  override def cloneType =
    new BiuIO().asInstanceOf[this.type]
}

class Biu(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new BiuIO)

  // To cut the potential comb loop and critical path between LSU and IFU
  //   and also core and external system, we always cut the ready by BIU Stage, no pipe.
  val master_arb = Module(new IcbArbiter(arbN = 2, outN = 1, pipe = false, flow = false))

  io.lsu <> master_arb.io.master(0)  // lsu has high priority
  io.ifu <> master_arb.io.master(1)

  val biu_buffer = Module(new IcbBuffer(cmdDepth = 1, rspDepth = 0, cmdPipe = false, cmdFlow = false, rspPipe = false, rspFlow = false)) /* but why rspDepth = 0? */

  biu_buffer.io.master <> master_arb.io.slave

  val slave_spl = Module(new IcbSpliter(splN = 5, outN = 1, pipe = false, flow = true))  //the splt is after the buffer, and will directly talk to the external bus, where maybe the ROM is 0 cycle responsed, so flow true.

  slave_spl.io.master <> biu_buffer.io.slave

  slave_spl.io.spl_id := Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.MEM_ADDR, 0.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.PPI_ADDR, 1.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.CLIC_ADDR, 2.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.PLIC_ADDR, 3.U, 4.U))))

  io.mem  <> slave_spl.io.slave(0)
  io.mem.cmd.bits.addr := slave_spl.io.slave(0).cmd.bits.addr >> 2
  io.ppi  <> slave_spl.io.slave(1)
  io.clic <> slave_spl.io.slave(2)
  io.plic <> slave_spl.io.slave(3)

  val biu_err = Wire(new IcbIO)
  biu_err  <> slave_spl.io.slave(4)
  biu_err.cmd.ready := true.B
  biu_err.rsp.valid := true.B
  biu_err.rsp.bits.err := true.B
  biu_err.rsp.bits.rdata := DontCare
}

