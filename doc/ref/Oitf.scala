package lingscale.cc01.core

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}
import chisel3.internal.naming._  // can't use chisel3_ version because of compile order

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

class Oitf(entries: Int = 2)(implicit val p: Parameters) extends Module with CoreParams {
  require(entries > 0, "Oitf must have posetive number of entries")

  val io = IO(new OitfIo)

  class out_info extends Bundle {
    val rs1en = Bool()
    val rs2en = Bool()
    val rdwen = Bool()
    val rs1idx = UInt(5.W)
    val rs2idx = UInt(5.W)
    val rdidx  = UInt(5.W)
    val pc = UInt(xlen.W)
  }

  private val buf = Reg(Vec(entries, new out_info))
  private val buf_vld = RegInit(VecInit(Seq.fill(entries)(false.B)))
  private val dis_ptr = Counter(entries)
  private val ret_ptr = Counter(entries)
  private val maybe_full = RegInit(false.B)

  private val ptr_match = dis_ptr.value === ret_ptr.value
  private val empty = ptr_match && !maybe_full
  private val full = ptr_match && maybe_full
  private val do_dis = io.dis_valid && io.dis_ready
  private val do_ret = io.ret_valid && io.ret_ready

  when (do_dis) {
    buf(dis_ptr.value).rs1en := io.dis_rs1en
    buf(dis_ptr.value).rs2en := io.dis_rs2en
    buf(dis_ptr.value).rdwen := io.dis_rdwen
    buf(dis_ptr.value).rs1idx := io.dis_rs1idx
    buf(dis_ptr.value).rs2idx := io.dis_rs2idx
    buf(dis_ptr.value).rdidx  := io.dis_rdidx
    buf(dis_ptr.value).pc := io.dis_pc
    buf_vld(dis_ptr.value) := true.B
    dis_ptr.inc()
  }

  when (do_ret) {
    buf_vld(ret_ptr.value) := false.B
    ret_ptr.inc()
  }

  io.ret_rdwen := buf(ret_ptr.value).rdwen
  io.ret_rdidx := buf(ret_ptr.value).rdidx
  io.ret_pc := buf(ret_ptr.value).pc

  when (do_dis =/= do_ret) {
    maybe_full := do_dis
  }

  io.dis_ready := !full
  io.ret_ready := !empty

  io.oitfrd_match_disrs1 := (buf_vld, buf, buf).zipped.map(_ && _.rdwen && io.dis_rs1en && _.rdidx === io.dis_rs1idx).reduce(_ || _)
  io.oitfrd_match_disrs2 := (buf_vld, buf, buf).zipped.map(_ && _.rdwen && io.dis_rs2en && _.rdidx === io.dis_rs2idx).reduce(_ || _)
  io.oitfrd_match_disrd  := (buf_vld, buf, buf).zipped.map(_ && _.rdwen && io.dis_rdwen && _.rdidx === io.dis_rdidx ).reduce(_ || _)

  io.empty := empty
}



