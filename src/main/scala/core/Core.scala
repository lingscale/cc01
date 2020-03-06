package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}


case object XLEN extends Field[Int]

abstract trait CoreParams {
  implicit val p: Parameters
  val xlen = p(XLEN)
}

class CoreIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val ext_irq = Input(Bool())
  val sft_irq = Input(Bool())
  val tmr_irq = Input(Bool())
  val ppi  = new IcbIO   // private perpheral interface
  val clic = new IcbIO   // core local interrupt controller
  val plic = new IcbIO   // platform-level interrupt controller
  val mem  = new IcbIO
}

class Core(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CoreIO)
  val datapath = Module(new Datapath) 
  val pre_ctrl  = Module(new Control)
  val ctrl  = Module(new Control)
  val itcm = Module(new Itcm)
  val dtcm = Module(new Dtcm)
  val biu  = Module(new Biu)

  datapath.io.ext_irq := io.ext_irq
  datapath.io.sft_irq := io.sft_irq
  datapath.io.tmr_irq := io.tmr_irq

  datapath.io.pre_ctrl <> pre_ctrl.io
  datapath.io.ctrl <> ctrl.io

  val ifu_buffer = Module(new IcbBuffer(cmdDepth = 0, rspDepth = 0, cmdPipe = false, cmdFlow = true, rspPipe = false, rspFlow = true))
  ifu_buffer.io.master <> datapath.io.ifu
  val ifu_spl = Module(new IcbSpliter(splN = 2, outN = 1, pipe = true, flow = false))
  ifu_spl.io.master <> ifu_buffer.io.slave
  ifu_spl.io.spl_id := Mux(datapath.io.ifu.cmd.bits.addr === AddrRegion.ITCM_ADDR, 0.U, 1.U)
  itcm.io.ifu <> ifu_spl.io.slave(0)
  biu.io.ifu <> ifu_spl.io.slave(1)

  // bypass lsu command channel here because: the ready signal (fe_ready) from execute stage will back-pressure the ifetch
  // response channel, if no such bypass, there maybe a deadlock: ifu rsp wait for execute stage to be cleared, while execute stage may
  // access biu or itcm and they waiting the ifu to accept last instruction access to make way of biu or itcm for lsu to access.
  val lsu_buffer = Module(new IcbBuffer(cmdDepth = 1, rspDepth = 0, cmdPipe = false, cmdFlow = true, rspPipe = false, rspFlow = true))
  lsu_buffer.io.master <> datapath.io.lsu
  val lsu_spl = Module(new IcbSpliter(splN = 3, outN = 1, pipe = true, flow = false))
  lsu_spl.io.master <> lsu_buffer.io.slave
  lsu_spl.io.spl_id := Mux(datapath.io.lsu.cmd.bits.addr === AddrRegion.DTCM_ADDR, 0.U,
                       Mux(datapath.io.lsu.cmd.bits.addr === AddrRegion.ITCM_ADDR, 1.U, 2.U))
  dtcm.io <> lsu_spl.io.slave(0)
  itcm.io.lsu <> lsu_spl.io.slave(1)
  biu.io.lsu <> lsu_spl.io.slave(2)

  io.ppi  <> biu.io.ppi
  io.clic <> biu.io.clic
  io.plic <> biu.io.plic
  io.mem  <> biu.io.mem

}



