package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters
import Control._

class ImmGenIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val inst = Input(UInt(xlen.W))
  val sel  = Input(UInt(3.W))
  val out  = Output(UInt(xlen.W))
}

class ImmGen(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ImmGenIO)

  val Iimm = io.inst(31, 20).asSInt // load, arithmetic, logic, jalr
  val Simm = Cat(io.inst(31, 25), io.inst(11, 7)).asSInt  // store
  val Bimm = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt // branch
  val Uimm = Cat(io.inst(31, 12), 0.U(12.W)).asSInt // lui, auipc
  //val Jimm = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt // jal
  //val Zimm = io.inst(19, 15).zext // CSR I
  val Imm4 = 4.S(xlen.W)

  io.out := MuxLookup(io.sel, Iimm & -2.S, Seq(IMM_I -> Iimm, IMM_S -> Simm, IMM_B -> Bimm, IMM_U -> Uimm, IMM_4 -> Imm4)).asUInt
}
