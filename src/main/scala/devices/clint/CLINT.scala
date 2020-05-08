package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

// Core Local Interrupts Controller

class CLINTIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val icb = Flipped(new IcbIO)
  val sft_irq = Output(Bool())
  val tmr_irq = Output(Bool())
  val clockB = Input(Clock())
}

class CLINT(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CLINTIO)

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U(xlen.W)
  io.icb.rsp.bits.err := false.B

  // soft interrupt

  val msip_addr = 0x02000000.U(32.W)  // software interrupt register

  val msip_sft_irq = RegInit(false.B)
  val msip = Cat(0.U(32.W), msip_sft_irq)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === msip_addr) && !io.icb.cmd.bits.read) {
    msip_sft_irq := io.icb.cmd.bits.wdata(0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === msip_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := msip
  }

  io.sft_irq := msip_sft_irq

  // timer interrupt

  val mtime_addr    = 0x0200bff8.U(32.W)  // timer register
  val mtimecmp_addr = 0x02004000.U(32.W)  // comparator register

  // Machine Timer Registers (mtime and mtimecmp)
  val mtime = Reg(UInt(64.W))  // memory-mapped machine-mode registers, not belong CSR
  val mtimecmp_lo = Reg(UInt(32.W))
  val mtimecmp_hi = Reg(UInt(32.W))
  val mtimecmp = Cat(mtimecmp_hi, mtimecmp_lo)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === mtime_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := mtime(31, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === (mtime_addr + 4.U)) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := mtime(63, 32)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === mtimecmp_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := mtimecmp(31, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === (mtimecmp_addr + 4.U)) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := mtimecmp(63, 32)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === mtimecmp_addr) && !io.icb.cmd.bits.read) {
    mtimecmp_lo := io.icb.cmd.bits.wdata
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === (mtimecmp_addr + 4.U)) && !io.icb.cmd.bits.read) {
    mtimecmp_hi := io.icb.cmd.bits.wdata
  }

  val tmr_irq = RegInit(false.B)

  when (mtime >= mtimecmp) { tmr_irq := true.B } .otherwise { tmr_irq := false.B }



  val mtime_raw_cdc = Wire(UInt(64.W))
 
  withClock (io.clockB) {
    val mtime_raw = RegInit(0.U(64.W))
    mtime_raw := mtime_raw + 1.U
    mtime_raw_cdc := mtime_raw
  }

  val mtime_pre = RegNext(mtime_raw_cdc, 0.U(64.W))

  when (mtime_pre === mtime_raw_cdc) { mtime := mtime_pre }



  io.tmr_irq := tmr_irq
}



