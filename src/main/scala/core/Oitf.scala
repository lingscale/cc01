package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

class OitfIo(implicit val p: Parameters) extends Bundle with CoreParams {
  val dis_valid = Input(Bool())
  val dis_ready = Output(Bool())
  val dis_rs1en = Input(Bool())
  val dis_rs2en = Input(Bool())
  val dis_rdwen = Input(Bool())
  val dis_rs1idx = Input(UInt(5.W))
  val dis_rs2idx = Input(UInt(5.W))
  val dis_rdidx  = Input(UInt(5.W))
  val dis_pc = Input(UInt(xlen.W))
  val oitfrd_match_disrs1 = Output(Bool())
  val oitfrd_match_disrs2 = Output(Bool())
  val oitfrd_match_disrd  = Output(Bool())

  val ret_valid = Input(Bool())
  val ret_ready = Output(Bool())
  val ret_rdwen = Output(Bool())
  val ret_rdidx = Output(UInt(5.W))
  val ret_pc = Output(UInt(xlen.W))

  val empty = Output(Bool())
}

class Oitf(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new OitfIo)

  val rs1en = Reg(Bool())
  val rs2en = Reg(Bool())
  val rdwen = Reg(Bool())
  val rs1idx = Reg(UInt(5.W))
  val rs2idx = Reg(UInt(5.W))
  val rdidx  = Reg(UInt(5.W))
  val pc = Reg(UInt(xlen.W))

  val empty = RegInit(true.B)

  when (io.dis_valid && io.dis_ready) {
    rs1en := io.dis_rs1en
    rs2en := io.dis_rs2en
    rdwen := io.dis_rdwen
    rs1idx := io.dis_rs1idx
    rs2idx := io.dis_rs2idx
    rdidx  := io.dis_rdidx
    pc := io.dis_pc
  }

  when (io.ret_valid && io.ret_ready) {
    empty := true.B
  }
  when (io.dis_valid && io.dis_ready) {
    empty := false.B
  }

  io.dis_ready := empty || io.ret_valid
  io.ret_ready := !empty

  io.oitfrd_match_disrs1 := !empty && rdwen && (rdidx === io.dis_rs1idx)
  io.oitfrd_match_disrs2 := !empty && rdwen && (rdidx === io.dis_rs2idx)
  io.oitfrd_match_disrd  := !empty && rdwen && (rdidx === io.dis_rdidx)

  io.ret_rdwen := rdwen
  io.ret_rdidx := rdidx
  io.ret_pc := pc

  io.empty := empty
}
