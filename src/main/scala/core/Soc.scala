package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

import chisel3.util.experimental.loadMemoryFromFile

class SocIO(implicit val p: Parameters) extends Bundle with CoreParams {

  val tx = Output(Bool())
  val rx = Input(Bool())

  val clockRTC = Input(Clock())

  val MOSI = Output(Bool())
  val MISO = Input(Bool())
  val SS   = Output(Bool())
  val SCK  = Output(Bool())
}

class Soc(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new SocIO)

  val core = Module(new Core)
//  loadMemoryFromFile(core.itcm.tcm.ram.mem, "src/main/resources/mem.txt")  // for simulation

  val clint = Module(new Clint)
//  val plic  = Module(new Plic)
  val ppi   = Module(new Ppi)
    val uart = Module(new Uart(UartAddr.UART_BASE_ADDR))
    val spi  = Module(new Spi(SpiAddr.SPI_BASE_ADDR))
  val rom   = Module(new Rom)

  core.io.clint <> clint.io.icb
//  core.io.plic  <> plic.io.icb
  core.io.ppi   <> ppi.io.icb
  core.io.mem   <> rom.io

  core.io.sft_irq := clint.io.sft_irq
  core.io.tmr_irq := clint.io.tmr_irq
  clint.io.clockRTC := io.clockRTC
 
  core.io.ext_irq := false.B
  core.io.plic.cmd.ready := true.B
  core.io.plic.rsp.valid := true.B
  core.io.plic.rsp.bits.rdata := 0.U
  core.io.plic.rsp.bits.err := false.B
/*
  core.io.ext_irq := plic.io.ext_irq
  plic.io.irq1 := false.B
  plic.io.irq2 := false.B
  plic.io.irq3 := uart.io.uart_irq
*/
  ppi.io.uart <> uart.io.icb
  ppi.io.spi  <> spi.io.icb

  io.tx := uart.io.tx
  uart.io.rx := io.rx

  io.MOSI := spi.io.MOSI
  spi.io.MISO := io.MISO
  io.SS := spi.io.SS
  io.SCK := spi.io.SCK

}



