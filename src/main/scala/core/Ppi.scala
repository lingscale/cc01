package lingscale.cc01.core

import chisel3._
import chisel3.util._
import scala.math.max
import scala.collection.mutable.ArraySeq
import lingscale.cc01.config.{Field, Parameters}

class PpiIO(implicit p: Parameters) extends Bundle {
  val icb = Flipped(new IcbIO)  // from bus interface unit
  val spi   = new IcbIO  // serial peripheral interface
  val uart  = new IcbIO  // universal asynchronous receiver/transmitter
  override def cloneType =
    new PpiIO().asInstanceOf[this.type]
}

class Ppi(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new PpiIO)

  //  a ping-pong buffer can be added to cut down the timing path
//  val ppi_buffer = Module(new IcbBuffer(cmdDepth = 2, rspDepth = 2, cmdPipe = false, cmdFlow = false, rspPipe = false, rspFlow = false))
  val ppi_buffer = Module(new IcbBuffer(cmdDepth = 0, rspDepth = 0, cmdPipe = false, cmdFlow = false, rspPipe = false, rspFlow = false))

  io.icb <> ppi_buffer.io.master

  val slave_spl = Module(new IcbSpliter(splN = 3, outN = 1, pipe = false, flow = true))

  ppi_buffer.io.slave <> slave_spl.io.master

  slave_spl.io.spl_id := Mux(ppi_buffer.io.slave.cmd.bits.addr === PpiRegion.UART_ADDR, 0.U,
                         Mux(ppi_buffer.io.slave.cmd.bits.addr === PpiRegion.SPI_ADDR, 1.U, 2.U))

  slave_spl.io.slave(0) <> io.uart
  slave_spl.io.slave(1) <> io.spi

  val ppi_err = Wire(new IcbIO)
  slave_spl.io.slave(2) <> ppi_err
  ppi_err.cmd.ready := true.B
  ppi_err.rsp.valid := true.B
  ppi_err.rsp.bits.err := true.B
  ppi_err.rsp.bits.rdata := DontCare
}

