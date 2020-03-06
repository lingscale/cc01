package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

trait UartParams {
  val dataBits = 8
  val stopBits = 1
  val divisorBits = 16
  val oversample = 4
  val nSamples = 3
  val nTxEntries = 8
  val nRxEntries = 8
}

// juse for test now, not well functioned.
class Uart(base_addr: Int)(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new Bundle{
    val icb = Flipped(new IcbIO)
    val tx = Output(Bool())
    val rx = Input(Bool())
  })

  val txdata_addr = (base_addr + 0x00).asUInt
  val rxdata_addr = (base_addr + 0x04).asUInt
  val txctrl_addr = (base_addr + 0x08).asUInt
  val rxctrl_addr = (base_addr + 0x0c).asUInt
  val ie_addr     = (base_addr + 0x10).asUInt
  val ip_addr     = (base_addr + 0x14).asUInt
  val div_addr    = (base_addr + 0x18).asUInt

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U(xlen.W)
  io.icb.rsp.bits.err := false.B

  val tx = RegInit(true.B)
  val tx_ptr = RegInit(0.U(4.W))

  val txdata_full = RegInit(false.B)
  val txdata_data = Reg(UInt(8.W))
  val txdata = Cat(txdata_full, 0.U(13.W), txdata_data)

  val div_div = Reg(UInt(16.W))   // 11.2896M / ( 195 + 1 ) = 57600, 0.0% error to 57600; 24.576M / ( 425 + 1 ) = 57690, 0.16% error to 57600; 33.8688M / ( 587 + 1 ) = 57600, 0.0% error to 57600; 50M / ( 867 + 1 ) = 57603.69, 0.006% error to 57600.
  val div = Cat(0.U(16.W), div_div)

  val counter = Reg(UInt(16.W))

  when (txdata_full) { counter := counter + 1.U } .otherwise { counter := 0.U }
  when (counter === div(15, 0)) { counter := 0.U }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txdata_addr) && !io.icb.cmd.bits.read) {
    txdata_data := io.icb.cmd.bits.wdata(7, 0)
    txdata_full := true.B
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === div_addr) && !io.icb.cmd.bits.read) {
    div_div := io.icb.cmd.bits.wdata(15, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txdata_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(txdata_full, 0.U(31.W))
  }

  val txdata_frame = Cat(1.U(1.W), txdata_data, 0.U(1.W))

  when (txdata_full && counter === div_div) {
    when (tx_ptr =/= 10.U) { tx := txdata_frame(tx_ptr); tx_ptr := tx_ptr + 1.U }
    .otherwise { tx := true.B; tx_ptr := 0.U; txdata_full := false.B }
  }

  io.tx := tx
}
