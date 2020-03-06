package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

import chisel3.util.experimental.loadMemoryFromFile

class SocIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val tx = Output(Bool())
}

class Soc(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new SocIO)

  val core = Module(new Core)
//  loadMemoryFromFile(core.itcm.tcm.ram.mem, "src/test/resources/mem.txt")

  val uart = Module(new Uart(UartAddr.UART_BASE_ADDR))
  val rom = Module(new Rom)

  core.io.ext_irq := false.B
  core.io.sft_irq := false.B
  core.io.tmr_irq := false.B

  io.tx := uart.io.tx
  uart.io.rx := DontCare

  core.io.ppi <> uart.io.icb
  core.io.mem <> rom.io

/*

  core.io.ppi.cmd.ready := true.B
  core.io.ppi.rsp.valid := false.B
  core.io.ppi.rsp.bits.rdata := 0.U(xlen.W)
  core.io.ppi.rsp.bits.err := true.B
*/
  core.io.clic.cmd.ready := DontCare
  core.io.clic.rsp.valid := DontCare
  core.io.clic.rsp.bits.rdata := DontCare
  core.io.clic.rsp.bits.err := DontCare

  core.io.plic.cmd.ready := DontCare
  core.io.plic.rsp.valid := DontCare
  core.io.plic.rsp.bits.rdata := DontCare
  core.io.plic.rsp.bits.err := DontCare

}



