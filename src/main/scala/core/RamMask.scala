package lingscale.cc01.core

import chisel3._
import chisel3.util._

class RamMaskIO(val addr_width: Int) extends Bundle {
  val dataOut = Output(UInt(32.W))
  val dataIn  = Input(UInt(32.W))
  val addr    = Input(UInt(32.W))
  val write   = Input(Bool())
  val mask    = Input(UInt(4.W))
  val enable  = Input(Bool())
}

class RamMask(val addr_width: Int, val ram_depth: Int) extends Module {
  val io = IO(new RamMaskIO(addr_width))

  val mem = SyncReadMem(ram_depth, Vec(4, UInt(8.W)))
  val dataOut = Wire(Vec(4, UInt(8.W)))
  val dataIn = Wire(Vec(4, UInt(8.W)))
  val mask = Wire(Vec(4, Bool()))

  dataIn(0) := io.dataIn( 7,  0)
  dataIn(1) := io.dataIn(15,  8)
  dataIn(2) := io.dataIn(23, 16)
  dataIn(3) := io.dataIn(31, 24)

  mask(0) := io.mask(0)
  mask(1) := io.mask(1)
  mask(2) := io.mask(2)
  mask(3) := io.mask(3)

  io.dataOut := dataOut.asUInt

  when (io.enable && io.write) { mem.write(io.addr(addr_width - 1, 2), dataIn, mask) }
  dataOut := mem.read(io.addr(addr_width - 1, 2), io.enable && !io.write)

}

