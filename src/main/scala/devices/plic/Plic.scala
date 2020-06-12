package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.Parameters

// Platform-Level Interrupt Controller with bad coding style, not well functionalized, just for temporary use.

class GatewayIO extends Bundle {
  val interrupt = Input(Bool())
  val valid = Output(Bool())
  val ready = Input(Bool())
  val complete = Input(Bool())
}

class Gateway extends Module {
  val io = IO(new GatewayIO)

  val inFlight = RegInit(false.B)
  when (io.interrupt && io.ready) { inFlight := true.B }
  when (io.complete) { inFlight := false.B }
  io.valid := io.interrupt && !inFlight
}

class PlicIO(implicit val p: Parameters) extends Bundle with CoreParams {
  val icb = Flipped(new IcbIO)
  val ext_irq = Output(Bool())
  val irq1 = Input(Bool())
  val irq2 = Input(Bool())
  val irq3 = Input(Bool())
}

class Plic(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new PlicIO)

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U(xlen.W)
  io.icb.rsp.bits.err := false.B

  val gateway1 = Module(new Gateway)
  val gateway2 = Module(new Gateway)
  val gateway3 = Module(new Gateway)

  // Interrupt Priorities registers

  val irq1_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq1_priority.U) && !io.icb.cmd.bits.read) {
    irq1_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq1_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq1_priority(2, 0))
  }

  val irq2_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq2_priority.U) && !io.icb.cmd.bits.read) {
    irq2_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq2_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq2_priority(2, 0))
  }

  val irq3_priority = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq3_priority.U) && !io.icb.cmd.bits.read) {
    irq3_priority := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.irq3_priority.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), irq3_priority(2, 0))
  }

  // Interrupt Pending Bits registers

  val pending = Reg(Vec(32, Bool()))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.pending.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := pending.asUInt
  }
  pending(0) := false.B

  // Interrupt Enables registers

  val enable = RegInit(0.U(32.W))  // interrupt enable retister
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.enable.U) && !io.icb.cmd.bits.read) {
    enable := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.enable.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(enable(31, 1), 0.U(1.W))
  }

  // Priority Thresholds register

  val threshold = Reg(UInt(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.threshold.U) && !io.icb.cmd.bits.read) {
    threshold := io.icb.cmd.bits.wdata
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.threshold.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), threshold(2, 0))
  }

  // Interrupt Claim/Completion registers

  val claim = RegInit(0.U(32.W))
  val complete = RegInit(0.U(32.W))
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.claim_complete.U) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := claim
  }
  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.claim_complete.U) && !io.icb.cmd.bits.read) {
    complete := io.icb.cmd.bits.wdata
  }

  gateway1.io.interrupt := io.irq1
  gateway2.io.interrupt := io.irq2
  gateway3.io.interrupt := io.irq3
  gateway1.io.ready := pending(1)
  gateway2.io.ready := pending(2)
  gateway3.io.ready := pending(3)
  gateway1.io.complete := complete(1)
  gateway2.io.complete := complete(2)
  gateway3.io.complete := complete(3)
 
  pending(1) := false.B
  pending(2) := false.B
  pending(3) := false.B
  when (gateway1.io.valid && enable(1) && (irq1_priority(2, 0) =/= 0.U)) { pending(1) := true.B }
  when (gateway2.io.valid && enable(2) && (irq2_priority(2, 0) =/= 0.U)) { pending(2) := true.B }
  when (gateway3.io.valid && enable(3) && (irq3_priority(2, 0) =/= 0.U)) { pending(3) := true.B }

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

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === PlicAddr.claim_complete.U) && io.icb.cmd.bits.read) {
    complete := ~claim
  }
    
  io.ext_irq := interrupt_request
}



