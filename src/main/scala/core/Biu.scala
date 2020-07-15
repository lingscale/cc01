package lingscale.cc01.core

import chisel3._
import chisel3.util._
import scala.math.max
import scala.collection.mutable.ArraySeq
import lingscale.cc01.config.{Field, Parameters}

class BiuIO(implicit p: Parameters) extends Bundle {
  val lsu = Flipped(new IcbIO)  // load store unit
  val ifu = Flipped(new IcbIO)  // instruction fetch unit
  val ppi   = new IcbIO  // private perpheral interface
  val clint = new IcbIO  // core local interrupt controller
  val plic  = new IcbIO  // platform-level interrupt controller
  val mem   = new IcbIO
  override def cloneType =
    new BiuIO().asInstanceOf[this.type]
}

class Biu(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new BiuIO)

  // To cut the potential comb loop and critical path between LSU and IFU
  //   and also core and external system, we always cut the ready by BIU Stage, no pipe.
  // Since biu can't pipe, so whether pipe ture or false for IcbArbiter or buf dosen't matter,
  //   but pipe will add extra logic, so not pipe for IcbArbiter and buf are better choice.

  // Because in BIU we always have cmdDepth larger than 0, when the response come back from the external bus, 
  //   it is at least 1 cycle later, so don't allow the 0 cycle response, flow be false.
  val master_arb = Module(new IcbArbiter(arbN = 2, outN = 1, pipe = false, flow = false))
//  val master_arb = Module(new IcbArbiter(arbN = 2, outN = 1, pipe = true, flow = true))
                                                                        /* Combinational loop detected */

  io.lsu <> master_arb.io.master(0)  // lsu has high priority
  io.ifu <> master_arb.io.master(1)

  val biu_buffer = Module(new IcbBuffer(cmdDepth = 1, rspDepth = 0, cmdPipe = false, cmdFlow = false, rspPipe = false, rspFlow = false))

  master_arb.io.slave <> biu_buffer.io.master

  val slave_spl = Module(new IcbSpliter(splN = 5, outN = 1, pipe = false, flow = true))  //the splt is after the buffer, and will directly talk to the external bus, where maybe the ROM is 0 cycle responsed, so flow true.
//  val slave_spl = Module(new IcbSpliter(splN = 5, outN = 1, pipe = true, flow = true))  //the splt is after the buffer, and will directly talk to the external bus, where maybe the ROM is 0 cycle responsed, so flow true.
                                                         /* Combinational loop detected */

  biu_buffer.io.slave <> slave_spl.io.master

  slave_spl.io.spl_id := Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.MEM_ADDR, 0.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.PPI_ADDR, 1.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.CLINT_ADDR, 2.U,
                         Mux(biu_buffer.io.slave.cmd.bits.addr === AddrRegion.PLIC_ADDR, 3.U, 4.U))))

  slave_spl.io.slave(0) <> io.mem
  slave_spl.io.slave(1) <> io.ppi
  slave_spl.io.slave(2) <> io.clint
  slave_spl.io.slave(3) <> io.plic

  val biu_err = Wire(new IcbIO)
  slave_spl.io.slave(4) <> biu_err
  biu_err.cmd.ready := true.B
  biu_err.rsp.valid := true.B
  biu_err.rsp.bits.err := true.B
  biu_err.rsp.bits.rdata := DontCare
}

