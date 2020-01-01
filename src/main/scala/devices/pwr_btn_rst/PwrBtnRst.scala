package lingscale.cc01.core

import chisel3._
import chisel3.util._

class PwrBtnRst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle{
    val rst_n = Output(Bool())
    val rst_btn_n = Input(Bool())
    val clk_50m = Input(Clock())
  })
  addResource("/PwrBtnRst.v")
}
