package lingscale.cc01.core

import chisel3._
import chisel3.util._


class Uart_fifo() extends Module {
  val io = IO(new Bundle{
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
    val write = Input(Bool())
    val read = Input(Bool())
    val empty = Output(Bool())
    val full = Output(Bool())
    val number = Output(UInt(3.W))
  })

  val buf = Reg(Vec(8, UInt(8.W)))
  val number = RegInit(0.U(3.W))
  val wr_ptr = Counter(8)
  val rd_ptr = Counter(8)
  val maybe_full = RegInit(false.B)

  val ptr_match = wr_ptr.value === rd_ptr.value
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full

  val write_ready = RegNext(!full, true.B)
  val read_valid = RegNext(!empty, false.B)

  when (io.write && write_ready) {
    buf(wr_ptr.value) := io.in
    number := number + 1.U
    wr_ptr.inc()
  }

  when (io.read && read_valid) {
    number := number - 1.U
    rd_ptr.inc()
  }

  when(io.write =/= io.read) {
    maybe_full := io.write
  }

  io.out := buf(rd_ptr.value)

  io.empty := empty
  io.full := full
  io.number := number
}
