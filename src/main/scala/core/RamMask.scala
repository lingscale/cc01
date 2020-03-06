package lingscale.cc01.core

import chisel3._
import chisel3.util._

class RamMaskIO(val addr_width: Int) extends Bundle {
  val dataOut = Output(UInt(32.W))
  val dataIn  = Input(UInt(32.W))
  val addr    = Input(UInt(32.W))
  val read    = Input(Bool())
  val write   = Input(Bool())
  val mask    = Input(UInt(4.W))
}

class RamMask(val addr_width: Int, val ram_depth: Int) extends Module {   // Single-ported SRAM
  val io = IO(new RamMaskIO(addr_width))

  val mem = SyncReadMem(ram_depth, Vec(4, UInt(8.W)))
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

// this style will cause instruction fetch combinational loop
  when (io.write) {
    mem.write(io.addr(addr_width-1+2, 0+2), dataIn, mask)
    io.dataOut := DontCare
  }
  .otherwise {
    io.dataOut := mem.read(io.addr(addr_width-1+2, 0+2), io.read).asUInt
  }

//  this style may not deduce single port ram
//  io.dataOut := mem.read(io.addr, io.read).asUInt
//  when (io.write) { mem.write(io.addr, dataIn, mask) }
}



