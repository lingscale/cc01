package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters
import scala.collection.immutable.ListMap

object CSR {
  val N = 0.U(2.W)  // not CSR operation
  val W = 1.U(2.W)  // write
  val S = 2.U(2.W)  // set
  val C = 3.U(2.W)  // clear

  // Supports machine modes
  val PRV_M = 0x3.U(2.W)

  // Machine-level CSR addrs
  // Machine Information Registers
  val mvendorid  = 0xF11.U(12.W)  // Vendor ID
  val marchid    = 0xF12.U(12.W)  // Architecture ID
  val mimpid     = 0xF13.U(12.W)  // Implementation ID
  val mhartid    = 0xF14.U(12.W)  // Hardware thread ID
  // Machine Trap Setup
  val mstatus    = 0x300.U(12.W)  // Machine status register
  val misa       = 0x301.U(12.W)  // ISA and extensions
  //val medeleg    = 0x302.U(12.W)  // Machine exception delegation register (only M-mode,should not exist)
  //val mideleg    = 0x303.U(12.W)  // Machine interrupt delegation register (only M-mode,should not exist)
  val mie        = 0x304.U(12.W)  // Machine interrupt-enable register
  val mtvec      = 0x305.U(12.W)  // Machine trap-handler base address
  val mcounteren = 0x306.U(12.W)  // Machine counter enable
  // Machine Trap Handling
  val mscratch   = 0x340.U(12.W)  // Scratch register for machine trap handlers
  val mepc       = 0x341.U(12.W)  // Machine exception program counter
  val mcause     = 0x342.U(12.W)  // Machine trap cause
  val mtval      = 0x343.U(12.W)  // Machine bad address or instruction
  val mip        = 0x344.U(12.W)  // Machine interrupt pending
  // Machine Counter/Timers
  val mcycle     = 0xB00.U(12.W)  // Machine cycle counter
  val minstret   = 0xB02.U(12.W)  // Machine intruction-retired counter
  val mcycleh    = 0xB80.U(12.W)  // Upper 32 bits of mcycle, RV32I only
  val minstreth  = 0xB82.U(12.W)  // Upper 32 bits of minstret, RV32I only
  // Machine Counter Setup
  val mcounterinhibit = 0x320.U(12.W) // Machine counter-inhibit register
/*
  val regs = List(  // for CSR tests
    mvendorid, marchid, mimpid, mhartid,
    mstatus, misa, mie, mtvec, mcounteren,
    mscratch, mepc, mcause, mtval, mip,
    mcycle, minstret, mcycleh, minstreth,
    mcounterinhibit)
*/
}

object Cause {
  val SupSftInpt          = 0x1.U  // Supervisor software interrupt
  val McnSftInpt          = 0x3.U  // Machine software interrupt
  val SupTmrInpt          = 0x5.U  // Supervisor timer interrupt
  val McnTmrInpt          = 0x7.U  // Machine timer interrupt
  val SupExtInpt          = 0x9.U  // Supervisor external interrupt
  val McnExtInpt          = 0xb.U  // Machine external interrupt
  val InstAddrMisaligned  = 0x0.U  // Instruction address misaligned
  val InstAccessFault     = 0x1.U  // Instruction access fault
  val IllegalInst         = 0x2.U  // Illegal instruction
  val Breakpoint          = 0x3.U  // Breakpoint
  val LoadAddrMisaligned  = 0x4.U  // Load address misaligned
  val LoadAccessFault     = 0x5.U  // Load access fault
  val StoreAddrMisaligned = 0x6.U  // Store/AMO address misaligned
  val StoreAccessFault    = 0x7.U  // Store/AMO access fault
  val EcallUMode          = 0x8.U  // Environment call from U-mode
  val EcallSMode          = 0x9.U  // Environment call from S-mode
  val EcallMMode          = 0xb.U  // Environment call from M-mode
  val Reserved            = 0xe.U  // Reserved for both interrupt and exception
}

class CSRIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val inst = Input(UInt(xlen.W))
  val rs1  = Input(UInt(xlen.W))
  val cmd = Input(UInt(2.W))
  val out  = Output(UInt(xlen.W))
  val mepc = Output(UInt(xlen.W))
  val mtvec = Output(UInt(xlen.W))
  val MEIE = Output(Bool())
  val MSIE = Output(Bool())
  val MTIE = Output(Bool())
  val MIE  = Output(Bool())
  val excp = Input(Bool())
  val cause = Input(UInt(xlen.W))
  val errpc = Input(UInt(xlen.W))
  val trapvalue = Input(UInt(xlen.W))
  val ret = Input(Bool())
}

class CSR(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CSRIO)

  // Machine Information Registers
  val misa       = Cat(1.U(2.W), 0.U((xlen - 28).W), (1 << ('I' - 'A')).U(26.W))   // read-write,  must be readable. only rve2i, machine mode supported
  val mvendorid  = 0.U(xlen.W)  // ready only, must be readable. not implemented
  val marchid    = 0.U(xlen.W)  // ready only, must be readable. not implemented
  val mimpid     = 0.U(xlen.W)  // must be readable. not implemented
  val mhartid    = 0.U(xlen.W)  // ready only, must be readable. only one hart

  // Machine Status Register (mstatus)
  val MIE  = RegInit(false.B)  // interrupt-enable bit
  val SIE  = false.B
  val UIE  = false.B
  val MPIE = RegInit(false.B)  // to support nested tarps
  val SPIE = false.B
  val UPIE = false.B
  val MPP  = 3.U(2.W)
  val SPP  = 0.U(1.W)
  //  UPP implicityly zero
  val MPRV = false.B           // memory privilege
  val MXR  = false.B
  val SUM  = false.B
  val TVM  = false.B           // virtualization support
  val TW   = false.B
  val TSR  = false.B
  val FS   = 0.U(2.W)          // extension context status
  val XS   = 0.U(2.W)
  val SD   = false.B           //   state dirty
  val mstatus = Cat(SD, 0.U(8.W), TSR, TW, TVM, MXR, SUM, MPRV, XS, FS, MPP, 0.U(2.W), SPP, MPIE, 0.U(1.W), SPIE, UPIE, MIE, 0.U(1.W), SIE, UIE)

  // Machine Trap-Vector Base-Address Register (mtvec)
  val mtvec = Const.PC_TVEC.U(xlen.W)  // do not support trap vector

  // Machine Interrupt Registers (mip and mie)
  val MTIP = RegInit(false.B)  // timer interrupt-pending bits
  val STIP = false.B
  val UTIP = false.B
  val MTIE = RegInit(false.B)  // timer interrupt-enable bits
  val STIE = false.B
  val UTIE = false.B
  val MSIP = RegInit(false.B)  // software interrupt-pending bits
  val SSIP = false.B
  val USIP = false.B
  val MSIE = RegInit(false.B)  // software interrupt-enable bits
  val SSIE = false.B
  val USIE = false.B
  val MEIP = RegInit(false.B)  // external interrupt-pending bits
  val SEIP = false.B
  val UEIP = false.B
  val MEIE = RegInit(false.B)  // external interrupt-enable bits
  val SEIE = false.B
  val UEIE = false.B
  val mip = Cat(0.U((xlen - 12).W), MEIP, 0.U(1.W), SEIP, UEIP, MTIP, 0.U(1.W), STIP, UTIP, MSIP, 0.U(1.W), SSIP, USIP)
  val mie = Cat(0.U((xlen - 12).W), MEIE, 0.U(1.W), SEIE, UEIE, MTIE, 0.U(1.W), STIE, UTIE, MSIE, 0.U(1.W), SSIE, USIE)

  // Machine Timer Registers (mtime and mtimecmp)
  // val mtime = Reg(UInt(64.W))  // memory-mapped machine-mode registers, not belong CSR
  // val mtimecmp = Reg(UInt(64.W))

  // Hardware Performance Monitor
  // mcycle minstret mcycleh minstreth mhpmcounter3-mphmcounter31  mhpmcounter3h-mphmcounter31h
  // all should be implementd, but can hard-wire the counter and its corresponding event selector to 0
  val mcycle    = 0.U(xlen.W)
  val minstret  = 0.U(xlen.W)
  val mcycleh   = 0.U(xlen.W)
  val minstreth = 0.U(xlen.W)

  // Machine Counter-Enable Register (mcounteren)
  val mcounteren = 0.U(xlen.W)  // availability of performance-monitoring counters to the next-lowest priviledged mode

  // Machine Counter-Inhibit Register (mcounterinhibit)
  val mcounterinhibit ="hFFFFFFFF".U(xlen.W)  // set to inhibit counter increment


  // Machine Scratch Register (mscratch)
  val mscratch = Reg(UInt(xlen.W))

  // Machine Exception Program Counter (mepc)
  val mepc = Reg(UInt(xlen.W))

  // Machine Cause Register (mcause)
  val mcause = Reg(UInt(xlen.W))

  // Machine Trap Value Register (mtval)
  val mtval = Reg(UInt(xlen.W))

  val csrFile = Seq(
    BitPat(CSR.mvendorid)       -> mvendorid,
    BitPat(CSR.marchid)         -> marchid,
    BitPat(CSR.mimpid)          -> mimpid,
    BitPat(CSR.mhartid)         -> mhartid,
    BitPat(CSR.mstatus)         -> mstatus,
    BitPat(CSR.misa)            -> misa,
    BitPat(CSR.mie)             -> mie,
    BitPat(CSR.mtvec)           -> mtvec,
    BitPat(CSR.mcounteren)      -> mcounteren,
    BitPat(CSR.mscratch)        -> mscratch,
    BitPat(CSR.mepc)            -> mepc,
    BitPat(CSR.mcause)          -> mcause,
    BitPat(CSR.mtval)           -> mtval,
    BitPat(CSR.mip)             -> mip,
    BitPat(CSR.mcycle)          -> mcycle,
    BitPat(CSR.minstret)        -> minstret,
    BitPat(CSR.mcycleh)         -> mcycleh,
    BitPat(CSR.minstreth)       -> minstreth,
    BitPat(CSR.mcounterinhibit) -> mcounterinhibit
  )

  val csr_addr = io.inst(31, 20)

  io.out := Lookup(csr_addr, 0.U, csrFile).asUInt
  io.mepc := mepc
  io.mtvec := mtvec
  io.MEIE := MEIE
  io.MSIE := MSIE
  io.MTIE := MTIE
  io.MIE  := MIE
 
  val wen = (io.cmd === CSR.W) || (io.cmd === CSR.S) || (io.cmd === CSR.C)
  val value = Mux(io.inst(14).asBool, io.inst(19, 15).zext.asUInt, io.rs1.asUInt)
  val wdata = MuxLookup(io.cmd, 0.U, Seq( CSR.W -> value, CSR.S -> (io.out | value),  CSR.C -> (io.out & ~value)))

  when (io.excp) {
    mepc := io.errpc >> 2 << 2
    mcause := io.cause
    mtval := io.trapvalue
    MIE := false.B  // mstatus.MIE
    MPIE := MIE  // mstatus.MPIE
  } .elsewhen (io.ret) {
    MIE := MPIE
    MPIE := true.B
  } .elsewhen (wen) {
    when (csr_addr === CSR.mstatus) {
      MIE := wdata(3)
      MPIE := wdata(7)
    } .elsewhen (csr_addr === CSR.mip) {
      MTIP := wdata(7)
      MSIP := wdata(3)
      MEIP := wdata(11)
    } .elsewhen (csr_addr === CSR.mie) {
      MTIE := wdata(7)
      MSIE := wdata(3)
      MEIE := wdata(11)
    } .elsewhen(csr_addr === CSR.mscratch) {
      mscratch := wdata
    } .elsewhen(csr_addr === CSR.mepc) {
      mepc := wdata
    } .elsewhen(csr_addr === CSR.mcause) {
      mcause := wdata
    } .elsewhen(csr_addr === CSR.mtval) {
      mtval := wdata
    }
  }
}



