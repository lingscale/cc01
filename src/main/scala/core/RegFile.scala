package lingscale.cc01.core

import chisel3._
import lingscale.cc01.config.Parameters

class RegFileIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val x1     = Output(UInt(xlen.W))
  val rs1    = Output(UInt(xlen.W))
  val rs2    = Output(UInt(xlen.W))
  val rs1idx = Input(UInt(5.W))
  val rs2idx = Input(UInt(5.W))
  val rd     = Input(UInt(xlen.W))
  val rdidx  = Input(UInt(5.W))
  val rdwen  = Input(Bool())
}

class RegFile(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new RegFileIO)
  val regs = Mem(32, UInt(xlen.W))
  io.rs1 := Mux(io.rs1idx.orR, regs(io.rs1idx), 0.U)
  io.rs2 := Mux(io.rs2idx.orR, regs(io.rs2idx), 0.U)
  io.x1 := regs(1.U)
  when (io.rdwen & io.rdidx.orR) {
    regs(io.rdidx) := io.rd
  }
}
