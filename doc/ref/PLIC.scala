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

  // Interrupt Priorities registers

  val irq1_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq1_priority.U) && !io.icb.cmd.bits.read) {
    irq1_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq1_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq1_priority(2, 0))
  }

  val irq2_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq2_priority.U) && !io.icb.cmd.bits.read) {
    irq2_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq2_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq2_priority(2, 0))
  }

  val irq3_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq3_priority.U) && !io.icb.cmd.bits.read) {
    irq3_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.irq3_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq3_priority(2, 0))
  }

  // Interrupt Enables registers

  val enable = RegInit(0.U(32.W))  // interrupt enable retister
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.enable.U) && !io.icb.cmd.bits.read) {
    enable := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.enable.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(enable(31, 1), 0.U(1.W))
  }

  // Interrupt Pending Bits registers

  val pending = Reg(Vec(32, Bool()))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.pending.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := pending.asUInt
  }

  // Priority Thresholds register

  val threshold = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.threshold.U) && !io.icb.cmd.bits.read) {
    threshold := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.threshold.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), threshold(2, 0))
  }

  // Interrupt Claim/Completion registers

  val claim = Reg(UInt(32.W))
  val complete = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.claim_complete.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := claim
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.claim_complete.U) && !io.icb.cmd.bits.read) {
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
      when ((io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.claim_complete.U) && io.icb.cmd.bits.read)) {
        state := s_COMPLETION
      }
      .otherwise {
        state := s_NOTIFICATION
      }
    }
    is (s_COMPLETION) {
      when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PLICAddr.claim_complete.U) && !io.icb.cmd.bits.read) {
        state := s_IDLE
      }
      .otherwise {
        state := s_COMPLETION
      }
    }
  } 

  io.ext_irq := interrupt_request
}



