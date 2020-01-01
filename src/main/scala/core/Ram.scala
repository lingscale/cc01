package lingscale.cc01.core

import chisel3._

class RamIO extends Bundle {
  val dataOut = Output(UInt(32.W))
  val dataIn  = Input(UInt(32.W))
  val addr    = Input(UInt(32.W))
  val read    = Input(Bool())
  val write   = Input(Bool())
}

class Ram extends Module {   // Single-ported SRAM
  val io = IO(new RamIO)
  val mem = SyncReadMem(2048, UInt(32.W))
  when (io.write) {
    mem.write(io.addr, io.dataIn)
    io.dataOut := DontCare
  }
  .otherwise {
    io.dataOut := mem.read(io.addr, io.read)
  }
}
