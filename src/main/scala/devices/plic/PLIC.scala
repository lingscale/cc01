package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

// Platform-Level Interrupt Controller with bad coding style, not well functionalized, just for temporary use.

class PLICIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val icb = Flipped(new IcbIO)
  val ext_irq = Output(Bool())
  val irq1 = Input(Bool())
  val irq2 = Input(Bool())
  val irq3 = Input(Bool())
}

class PLIC(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new PLICIO)

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U(xlen.W)
  io.icb.rsp.bits.err := false.B

  val irq1_priority_addr = (0x0C000000 + 1 * 4).U(32.W)  // interrupt 1 priority retister
  val irq1_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq1_priority_addr) && !io.icb.cmd.bits.read) {
    irq1_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq1_priority_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq1_priority(2, 0))
  }

  val irq2_priority_addr = (0x0C000000 + 2 * 4).U(32.W)  // interrupt 2 priority retister
  val irq2_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq2_priority_addr) && !io.icb.cmd.bits.read) {
    irq2_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq2_priority_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq2_priority(2, 0))
  }

  val irq3_priority_addr = (0x0C000000 + 3 * 4).U(32.W)  // interrupt 3 priority retister
  val irq3_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq3_priority_addr) && !io.icb.cmd.bits.read) {
    irq3_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === irq3_priority_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq3_priority(2, 0))
  }

  val enable_addr = 0x0C002000.U(32.W)  // interrupt enable retister
  val enable = RegInit(0.U(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === enable_addr) && !io.icb.cmd.bits.read) {
    enable := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === enable_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(enable(31, 1), 0.U(1.W))
  }

  val pending_addr = 0x0C001000.U(32.W)  // interrupt pending retister
  val pending = Reg(Vec(32, Bool()))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === pending_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := pending.asUInt
  }

  val threshold_addr = 0x0C002000.U(32.W)  // interrupt priority threshold retister
  val threshold = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === threshold_addr) && !io.icb.cmd.bits.read) {
    threshold := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === threshold_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), threshold(2, 0))
  }

  val claim_addr = 0x0C200004.U(32.W)  // claim retister
  val complete_addr = 0x0C200004.U(32.W)  // complete retister
  val claim = Reg(UInt(32.W))
  val complete = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === claim_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := claim
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === complete_addr) && !io.icb.cmd.bits.read) {
    complete := io.icb.cmd.bits.wdata
  }

  pending(0) := false.B
  when (io.irq1 && enable(1) && (irq1_priority(2, 0) =/= 0.U)) { pending(1) := true.B }
  when (io.irq2 && enable(2) && (irq2_priority(2, 0) =/= 0.U)) { pending(2) := true.B }
  when (io.irq3 && enable(3) && (irq3_priority(2, 0) =/= 0.U)) { pending(3) := true.B }

  when (pending(3)) { claim := 1.U << 3 }
  when (pending(2)) { claim := 1.U << 2 }
  when (pending(1)) { claim := 1.U << 1 }
  when (pending(3) && pending(2) && irq3_priority(2, 0) > irq2_priority(2, 0)) { claim := 1.U << 3 }
  when (pending(3) && pending(1) && irq3_priority(2, 0) > irq1_priority(2, 0)) { claim := 1.U << 3 }
  when (pending(2) && pending(1) && irq2_priority(2, 0) > irq1_priority(2, 0)) { claim := 1.U << 2 }
  when (pending(3) && pending(2) && pending(1) && irq3_priority(2, 0) > irq2_priority(2, 0) && irq3_priority(2, 0) > irq1_priority(2, 0))
    { claim := 1.U << 3 }
  when (!pending(3) && !pending(2) && !pending(1)) { claim := 0.U }


  val interrupt_request = ((claim === 1.U << 1 && irq1_priority(2, 0) > threshold(2, 0))
                       ||  (claim === 1.U << 2 && irq2_priority(2, 0) > threshold(2, 0))
                       ||  (claim === 1.U << 3 && irq3_priority(2, 0) > threshold(2, 0)))

  val s_IDLE :: s_NOTIFICATION :: s_CLAIM :: s_COMPLETION :: Nil = Enum(4)
  val state = RegInit(s_IDLE)

  switch (state) {
    is (s_IDLE) {
      when (interrupt_request) {
        state := s_NOTIFICATION
      }
      .otherwise {
        state := s_IDLE
      }
    }
    is (s_NOTIFICATION) {
      when ((io.icb.cmd.fire && (io.icb.cmd.bits.addr === claim_addr) && io.icb.cmd.bits.read)) {
        state := s_COMPLETION
      }
      .otherwise {
        state := s_NOTIFICATION
      }
    }
    is (s_COMPLETION) {
      when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === complete_addr) && !io.icb.cmd.bits.read) {
        state := s_IDLE
      }
      .otherwise {
        state := s_COMPLETION
      }
    }
  } 

  io.ext_irq := interrupt_request
}



