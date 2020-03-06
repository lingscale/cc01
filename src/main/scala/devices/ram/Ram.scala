package lingscale.cc01.core

import chisel3._
import chisel3.util._

class BlackBoxSpram extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle{
    val dataOut = Output(UInt(32.W))
    val dataIn  = Input(UInt(32.W))
    val addr    = Input(UInt(32.W))
    val read    = Input(Bool())
    val write   = Input(Bool())
    val mask    = Input(UInt(4.W))
    val clk     = Input(Clock())
  })
  addResource("/BlackBoxSpram.v")
}

class RamIO(val addr_width: Int) extends Bundle {
  val dataOut = Output(UInt(32.W))
  val dataIn  = Input(UInt(32.W))
  val addr    = Input(UInt(32.W))
  val read    = Input(Bool())
  val write   = Input(Bool())
  val mask    = Input(UInt(4.W))
}

class Ram(val addr_width: Int, val ram_depth: Int) extends Module {   // Single-ported SRAM
  val io = IO(new RamIO(addr_width))

  require(addr_width <= 16, "Constrained by the capacitance of iCE40UP5K SPRAM")

  val ram = Module(new BlackBoxSpram)

  io.dataOut    := ram.io.dataOut
  ram.io.dataIn := io.dataIn
  ram.io.addr   := io.addr
  ram.io.read   := io.read
  ram.io.write  := io.write
  ram.io.mask   := io.mask
  ram.io.clk    := clock

}
