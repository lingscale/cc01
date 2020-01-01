package lingscale.cc01.core

import chisel3._
import chisel3.util.ListLookup
import lingscale.cc01.config.Parameters
// import chisel3.util.Enum

object Control {
  val Y = true.B
  val N = false.B

  // A_sel
  val A_PC   = 0.U(1.W)
  val A_RS1  = 1.U(1.W)
  val A_XXX  = 1.U(1.W)

  // B_sel
  val B_IMM  = 0.U(1.W)
  val B_RS2  = 1.U(1.W)
  val B_XXX  = 1.U(1.W)

  // imm_sel
  val IMM_X  = 0.U(3.W)
  val IMM_I  = 1.U(3.W)
  val IMM_S  = 2.U(3.W)
  val IMM_B  = 3.U(3.W)
  val IMM_U  = 4.U(3.W)
  val IMM_4  = 5.U(3.W)

  // br_type
  val BR_XXX = 0.U(3.W)
  val BR_LTU = 1.U(3.W)
  val BR_LT  = 2.U(3.W)
  val BR_EQ  = 3.U(3.W)
  val BR_GEU = 4.U(3.W)
  val BR_GE  = 5.U(3.W)
  val BR_NE  = 6.U(3.W)

  // ld_type
  val LD_XXX = 0.U(3.W)
  val LD_LW  = 1.U(3.W)
  val LD_LH  = 2.U(3.W)
  val LD_LB  = 3.U(3.W)
  val LD_LHU = 4.U(3.W)
  val LD_LBU = 5.U(3.W)

  // st_type
  val ST_XXX = 0.U(2.W)
  val ST_SW  = 1.U(2.W)
  val ST_SH  = 2.U(2.W)
  val ST_SB  = 3.U(2.W)

  // wb_sel
  val WB_ALU = 0.U(2.W)
  val WB_MEM = 1.U(2.W)
  val WB_PC4 = 2.U(2.W)
  val WB_CSR = 3.U(2.W)
  // val wb_alu :: wb_mem :: wb_pc4 :: wb_csr :: Nil = Enum(4)

  import Instructions._
  import ALU._  // some logic in alu unit denpends on bits order of alu_op, so put alu_op in ALU.scala

  // pre_decode need rs1idx, rs2idx, bjp_imm
  // post_decode need rs1x0, rs2x0, rdidx, imm, dec_info?


  val default =
    //             need_rd      jalr
    //         need_rs2 |    jal |  fencei ebreak wfi                                                                     illegal?
    //      need_rs1 |  |  br |  |fence|ecall| mret| A_sel   B_sel  imm_sel   alu_op   br_type ld ld_type st st_type csr csr_cmd |
    //            |  |  |  |  |  |  |  |  |  |  |  |   |       |     |          |          |    |  |       |    |     |   |     |
             List(N, N, N, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, Y) /* addi x0, x0, 0 */
  val map = Array(
    LUI   -> List(N, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_IMM, IMM_U, ALU_COPY_B, BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    AUIPC -> List(N, N, Y, N, N, N, N, N, N, N, N, N, A_PC ,  B_IMM, IMM_U, ALU_ADD   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    JAL   -> List(N, N, Y, N, Y, N, N, N, N, N, N, N, A_PC ,  B_IMM, IMM_4, ALU_ADD   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    JALR  -> List(Y, N, Y, N, N, Y, N, N, N, N, N, N, A_PC ,  B_IMM, IMM_4, ALU_ADD   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BEQ   -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SUB   , BR_EQ , N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BNE   -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SUB   , BR_NE , N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BLT   -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SLT   , BR_LT , N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BGE   -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SLT   , BR_GE , N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BLTU  -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SLTU  , BR_LTU, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    BGEU  -> List(Y, Y, N, Y, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_B, ALU_SLTU  , BR_GEU, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    LB    -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, LD_LB , N, ST_XXX, N, CSR.N, N),
    LH    -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, LD_LH , N, ST_XXX, N, CSR.N, N),
    LW    -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, LD_LW , N, ST_XXX, N, CSR.N, N),
    LBU   -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, LD_LBU, N, ST_XXX, N, CSR.N, N),
    LHU   -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, LD_LHU, N, ST_XXX, N, CSR.N, N),
    SB    -> List(Y, Y, N, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, LD_XXX, Y, ST_SB , N, CSR.N, N),
    SH    -> List(Y, Y, N, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, LD_XXX, Y, ST_SH , N, CSR.N, N),
    SW    -> List(Y, Y, N, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, LD_XXX, Y, ST_SW , N, CSR.N, N),
    ADDI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLTI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_SLT   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLTIU -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_SLTU  , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    XORI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_XOR   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    ORI   -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_OR    , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    ANDI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_AND   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLLI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_SLL   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SRLI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_SRL   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SRAI  -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_IMM, IMM_I, ALU_SRA   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    ADD   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SUB   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SUB   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLL   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SLL   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLT   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SLT   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SLTU  -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SLTU  , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    XOR   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_XOR   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SRL   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SRL   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    SRA   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_SRA   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    OR    -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_OR    , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    AND   -> List(Y, Y, Y, N, N, N, N, N, N, N, N, N, A_RS1,  B_RS2, IMM_X, ALU_AND   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    FENCE -> List(N, N, N, N, N, N, Y, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    FENCEI-> List(N, N, N, N, N, N, N, Y, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    CSRRW -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.W, N),
    CSRRS -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.S, N),
    CSRRC -> List(Y, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.C, N),
    CSRRWI-> List(N, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.W, N),
    CSRRSI-> List(N, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.S, N),
    CSRRCI-> List(N, N, Y, N, N, N, N, N, N, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.C, N),
    ECALL -> List(N, N, N, N, N, N, N, N, Y, N, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    EBREAK-> List(N, N, N, N, N, N, N, N, N, Y, N, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    MRET  -> List(N, N, N, N, N, N, N, N, N, N, Y, N, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N),
    WFI   -> List(N, N, N, N, N, N, N, N, N, N, N, Y, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, LD_XXX, N, ST_XXX, N, CSR.N, N))
}

class ControlSignals(implicit val p: Parameters) extends Bundle with CoreParams {

  val inst     = Input(UInt(xlen.W))
  val need_rs1 = Output(Bool())
  val need_rs2 = Output(Bool())
  val need_rd  = Output(Bool())
  val br       = Output(Bool())
  val jal      = Output(Bool())
  val jalr     = Output(Bool())
  val fence    = Output(Bool())
  val fencei   = Output(Bool())
  val ecall    = Output(Bool())
  val ebreak   = Output(Bool())
  val mret     = Output(Bool())
  val wfi      = Output(Bool())
  val A_sel    = Output(UInt(1.W))
  val B_sel    = Output(UInt(1.W))
  val imm_sel  = Output(UInt(3.W))
  val alu_op   = Output(UInt(4.W))
  val br_type  = Output(UInt(3.W))
  val ld       = Output(Bool())
  val ld_type  = Output(UInt(3.W))
  val st       = Output(Bool())
  val st_type  = Output(UInt(2.W))
  val csr      = Output(Bool())
  val csr_cmd  = Output(UInt(2.W))
  val illegal  = Output(Bool())

}

class Control(implicit p: Parameters) extends Module {
  val io = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  io.need_rs1 := ctrlSignals(  0 )
  io.need_rs2 := ctrlSignals(  1 )
  io.need_rd  := ctrlSignals(  2 )
  io.br       := ctrlSignals(  3 )
  io.jal      := ctrlSignals(  4 )
  io.jalr     := ctrlSignals(  5 )
  io.fence    := ctrlSignals(  6 )
  io.fencei   := ctrlSignals(  7 )
  io.ecall    := ctrlSignals(  8 )
  io.ebreak   := ctrlSignals(  9 )
  io.mret     := ctrlSignals( 10 )
  io.wfi      := ctrlSignals( 11 )
  io.A_sel    := ctrlSignals( 12 )
  io.B_sel    := ctrlSignals( 13 )
  io.imm_sel  := ctrlSignals( 14 )
  io.alu_op   := ctrlSignals( 15 )
  io.br_type  := ctrlSignals( 16 )
  io.ld       := ctrlSignals( 17 )
  io.ld_type  := ctrlSignals( 18 )
  io.st       := ctrlSignals( 19 )
  io.st_type  := ctrlSignals( 20 )
  io.csr      := ctrlSignals( 21 )
  io.csr_cmd  := ctrlSignals( 22 )
  io.illegal  := ctrlSignals( 23 )
}


 





/*
  // RV32I Base Integer Instruction Set, Version 2.0  47 + 1
  def LUI     // lui   rd, imm[31:12]    load upper immediate           | imm[31:12] | rd | opcode |
  def AUIPC   // auipc rd, imm[31:12]    add upper immediate to pc
  def JAL     // jal  rd, imm[20:1]        jump and link                | imm[20] | imm[10:1] | imm[11] | imm[19:12] | rd | opcode |
  def JALR    // jalr rd, rs1, imm[11:0]   jump and link register      | imm[11:0] | rs1 | funct3 | rd | opcode |
  def BEQ     // beq  rs1, rs2, imm[12:1]   branch equal                | imm[12] | imm[10:5] | rs2 | rs1 | funct3 | imm[4:1] | imm[11] | opcode |
  def BNE     // bne  rs1, rs2, imm[12:1]   branch not equal
  def BLT     // blt  rs1, rs2, imm[12:1]   branch if rs1 is less than rs2, signed
  def BGE     // bge  rs1, rs2, imm[12:1]   branch if rs1 is greater or equal to rs2, signed
  def BLTU    // bltu rs1, rs2, imm[12:1]   branch if rs1 is less than rs2, unsigned
  def BGEU    // bgeu rs1, rs2, imm[12:1]   branch if rs1 is greater or equal to rs2, unsigned
  def LB      // lb  rd, offset[11:0](rs1)        | imm[11:0] | rs1 | funct3 | rd | opcode |
  def LH      // lh  rd, offset[11:0](rs1)
  def LW      // lw  rd, offset[11:0](rs1)
  def LBH     // lbh rd, offset[11:0](rs1)
  def LHU     // lhu rd, offset[11:0](rs1)
  def SB      // sb  rs2, offset[11:0](rs1)        | imm[11:5] | rs2 | rs1 | funct3 | imm[4:0] | opcode |
  def SH      // sh  rs2, offset[11:0](rs1)
  def SW      // sw  rs2, offset[11:0](rs1)
  def ADDI    // addi   rd, rs1, imm[11:0]    | imm[11:0] | rs1 | funct3 | rd | opcode |
  def SLTI    // slti   rd, rs1, imm[11:0]
  def SLTIU   // sltiu  rd, rs1, imm[11:0]
  def XORI    // xori   rd, rs1, imm[11:0]
  def ORI     // ori    rd, rs1, imm[11:0]
  def ANDI    // andi   rd, rs1, imm[11:0]
  def SLLI    // slli   rd, rs1, shamt[4:0]   | imm[11:5] | imm[4:0] | rs1 | funct3 | rd | opcode |
  def SRLI    // srli   rd, rs1, shamt[4:0]
  def SRAI    // srai   rd, rs1, shamt[4:0]
  def ADD     // add  rd, rs1, rs2             | funct7 | rs2 | rs1 | funct3 | rd | opcode |
  def SUB     // sub  rd, rs1, rs2
  def SLL     // sll  rd, rs1, rs2[4:0]
  def SLT     // slt  rd, rs1, rs2
  def SLTU    // sltu rd, rs1, rs2
  def XOR     // xor  rd, rs1, rs2
  def SRL     // srl  rd, rs1, rs2[4:0]
  def SRA     // sra  rd, rs1, rs2[4:0]
  def OR      // or   rd, rs1, rs2
  def AND     // and  rd, rs1, rs2
  def FENCE   // fence              |           | rs1 | funct3 | rd | opcode |
  def FENCEI  // fence.i            | imm[11:0] | rs1 | funct3 | rd | opcode |
  def ECALL   // ecall      environment call        | funct12 | rs1 | funct3 | rd | opcode |
  def EBREAK  // ebreak     environment breakpoint
  def CSRRW   // csrrw  rd, csr, rs1         | csr | rs1 | funct3 | rd | opcode |
  def CSRRS   // csrrs  rd, csr, rs1
  def CSRRC   // csrrc  rd, csr, rs1
  def CSRRWI  // csrrwi rd, csr, imm[4:0]
  def CSRRSI  // csrrwi rd, csr, imm[4:0]
  def CSRRCI  // csrrwi rd, csr, imm[4:0]

  def NOP     = BitPat("b00000000000000000000000000010011")   // ADDI x0, x0, 0

  // RISC-V Privileged Architectures V1.10
  // Machine-Mode Privileged Instructions  6?
//def ECALL   // belongs to RV32I?             | funct12 | rs1 | funct3 | rd | opcode |
//def EBREAK  // belongs to RV32I?
  def URET    // uret
  def SRET    // sret
  def MRET    // mret
  def WFI     // wfi      wait for interrupt

*/

