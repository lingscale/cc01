package lingscale.cc01.core

import chisel3._
import chisel3.util._
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
    val rdwen = Bool()
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

  private val dis_ready = !full
  private val ret_ready = !empty

  private val do_dis = io.dis_valid && io.dis_ready
  private val do_ret = io.ret_valid && io.ret_ready

  when (do_dis) {
    buf(dis_ptr.value).rdwen := io.dis_rdwen
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

  io.oitfrd_match_disrs1 := io.dis_rs1en && (buf_vld zip buf).map{ case (a, b) => a && b.rdwen && b.rdidx === io.dis_rs1idx}.reduce(_ || _)
  io.oitfrd_match_disrs2 := io.dis_rs2en && (buf_vld zip buf).map{ case (a, b) => a && b.rdwen && b.rdidx === io.dis_rs2idx}.reduce(_ || _)
  io.oitfrd_match_disrd  := io.dis_rdwen && (buf_vld zip buf).map{ case (a, b) => a && b.rdwen && b.rdidx === io.dis_rdidx }.reduce(_ || _)
  //io.oitfrd_match_disrs1 := io.dis_rs1en && (buf_vld, buf, buf).zipped.map(_ && _.rdwen && _.rdidx === io.dis_rs1idx).reduce(_ || _)
  //io.oitfrd_match_disrs2 := io.dis_rs2en && (buf_vld, buf, buf).zipped.map(_ && _.rdwen && _.rdidx === io.dis_rs2idx).reduce(_ || _)
  //io.oitfrd_match_disrd  := io.dis_rdwen && (buf_vld, buf, buf).zipped.map(_ && _.rdwen && _.rdidx === io.dis_rdidx ).reduce(_ || _)

  io.dis_ready := dis_ready
  io.ret_ready := ret_ready
  io.empty := empty
}

