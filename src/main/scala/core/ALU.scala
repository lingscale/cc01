package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

object ALU {
  val ALU_ADD    = 0.U(4.W)    // 0 0 0 0
  val ALU_SUB    = 1.U(4.W)    // 0 0 0 1
  val ALU_AND    = 2.U(4.W)    // 0 0 1 0
  val ALU_OR     = 3.U(4.W)    // 0 0 1 1
  val ALU_XOR    = 4.U(4.W)    // 0 1 0 0
  val ALU_SLT    = 5.U(4.W)    // 0 1 0 1
  val ALU_SLL    = 6.U(4.W)    // 0 1 1 0
  val ALU_SLTU   = 7.U(4.W)    // 0 1 1 1
  val ALU_SRL    = 8.U(4.W)    // 1 0 0 0
  val ALU_SRA    = 9.U(4.W)    // 1 0 0 1
  val ALU_COPY_B = 10.U(4.W)   // 1 0 1 0
  val ALU_XXX    = 15.U(4.W)   // 1 1 1 1
}

class ALUIo(implicit val p: Parameters) extends Bundle with CoreParams {
  val A = Input(UInt(xlen.W))
  val B = Input(UInt(xlen.W))
  val alu_op = Input(UInt(4.W))
  val br_type = Input(UInt(3.W))
  val out = Output(UInt(xlen.W))
  val br_rslv = Output(Bool())
}

import ALU._
import Control._  // br_type

class ALU(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ALUIo)

  val add_en   = io.alu_op === ALU_ADD || io.alu_op === ALU_SUB || io.alu_op ===  ALU_SLT || io.alu_op ===  ALU_SLTU  // to save power
  val shift_en = io.alu_op === ALU_SRA || io.alu_op === ALU_SRL || io.alu_op === ALU_SLL
  val and_en   = io.alu_op === ALU_AND
  val or_en    = io.alu_op === ALU_OR
  val xor_en   = io.alu_op === ALU_XOR || io.br_type === BR_EQ || io.br_type === BR_NE

  val sum = Mux(add_en, io.A, 0.U) + Mux(add_en, Mux(io.alu_op(0), -io.B, io.B), 0.U)
  val cmp = Mux(io.A(xlen-1) === io.B(xlen-1), sum(xlen-1),
            Mux(io.alu_op(1), io.B(xlen-1), io.A(xlen-1)))
  val shamt  = Mux(shift_en, io.B(4,0), 0.U)
  val shin   = Mux(io.alu_op(3), io.A, Reverse(io.A))
  val shiftr = (Cat(io.alu_op(0) && shin(xlen-1), shin).asSInt >> shamt)(xlen-1, 0)
  val shiftl = Reverse(shiftr)
  val and_res = Mux(and_en, io.A, 0.U) & Mux(and_en, io.B, 0.U)
  val or_res  = Mux(or_en, io.A, 0.U) | Mux(or_en, io.B, 0.U)
  val xor_res = Mux(xor_en, io.A, 0.U) ^ Mux(xor_en, io.B, 0.U)

  val out = 
    Mux(io.alu_op === ALU_ADD || io.alu_op === ALU_SUB, sum,
    Mux(io.alu_op === ALU_SLT || io.alu_op === ALU_SLTU, cmp,
    Mux(io.alu_op === ALU_SRA || io.alu_op === ALU_SRL, shiftr,
    Mux(io.alu_op === ALU_SLL, shiftl,
    Mux(io.alu_op === ALU_AND, and_res,
    Mux(io.alu_op === ALU_OR,  or_res,
    Mux(io.alu_op === ALU_XOR, xor_res, 
    Mux(io.alu_op === ALU_COPY_B, io.B, io.A))))))))

  val neq = xor_res.orR

  val br_rslv =
    Mux(io.br_type === BR_EQ, ~neq,
    Mux(io.br_type === BR_NE, neq,
    Mux(io.br_type === BR_LT, cmp,
    Mux(io.br_type === BR_GE, ~cmp,
    Mux(io.br_type === BR_LTU, cmp,
    Mux(io.br_type === BR_GEU, ~cmp, 0.U)))))).asBool


  io.out := out
  io.br_rslv := br_rslv
}
