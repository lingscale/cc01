package lingscale.cc01.core

import chisel3._
import chisel3.util._
import scala.math.max
import scala.collection.mutable.ArraySeq
import lingscale.cc01.config.{Field, Parameters}

case object IcbKey extends Field[IcbParameters]

case class IcbParameters(addrBits: Int, dataBits: Int)

trait HasIcbParameters {
  implicit val p: Parameters
  val icbExternal = p(IcbKey)
  val icbAddrBits = icbExternal.addrBits
  val icbDataBits = icbExternal.dataBits
  val icbMaskBits = icbDataBits / 8
}

class IcbCommandChannel(implicit val p: Parameters) extends Bundle with HasIcbParameters {
  val addr  = UInt(icbAddrBits.W)
  val read  = Bool()
  val wdata = UInt(icbDataBits.W)
  val wmask = UInt(icbMaskBits.W)
}

object IcbCommandChannel {
  def apply(addr: UInt, read: Bool, wdata: UInt, wmask: UInt)(implicit p: Parameters) = {
    val cmd = Wire(new IcbCommandChannel)
    cmd.addr  := addr
    cmd.read  := read
    cmd.wdata := wdata
    cmd.wmask := wmask
    cmd
  }
}

class IcbResponseChannel(implicit val p: Parameters) extends Bundle with HasIcbParameters {
  val rdata = UInt(icbDataBits.W)
  val err   = Bool()
}

object IcbResponseChannel {
  def apply(rdata: UInt, err: Bool)(implicit p: Parameters) = {
    val rsp = Wire(new IcbResponseChannel)
    rsp.rdata := rdata
    rsp.err   := err
    rsp
  }
}

// ICB Internal Chip Bus protocal, first used in Hummingbird E203 Opensource Processor Core, defined by Bob Hu (胡振波).

class IcbIO(implicit val p: Parameters) extends Bundle {
  val cmd = Decoupled(new IcbCommandChannel)
  val rsp = Flipped(Decoupled(new IcbResponseChannel))
}


class IcbArbiterIO(arbN: Int)(implicit p: Parameters) extends Bundle {
  val master = Flipped(Vec(arbN, new IcbIO))
  val slave = new IcbIO
  override def cloneType =
    new IcbArbiterIO(arbN).asInstanceOf[this.type]
}

/* Arbitrate among arbN masters requesting to a single slave */

class IcbArbiter(val arbN: Int, val outN: Int = 1, val pipe: Boolean = true, val flow: Boolean = false)(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new IcbArbiterIO(arbN))

  if (arbN > 1) {
    val cmd_arb = Module(new Arbiter(new IcbCommandChannel, arbN))
    val id_queue = Module(new Queue(chiselTypeOf(cmd_arb.io.chosen), entries = outN, pipe = pipe, flow = flow))

    id_queue.io.enq.bits := cmd_arb.io.chosen

    id_queue.io.enq.valid := io.slave.cmd.fire
    id_queue.io.deq.ready := io.slave.rsp.fire

    val id_indic = id_queue.io.deq.bits

    for (i <- 0 until arbN) {
      val m_cmd = io.master(i).cmd
      val a_cmd = cmd_arb.io.in(i)

      a_cmd <> m_cmd

      val m_rsp = io.master(i).rsp

      m_rsp.valid := io.slave.rsp.valid && id_queue.io.deq.valid && (id_indic === i.asUInt)
      m_rsp.bits  := io.slave.rsp.bits
    }

    io.slave.cmd.valid   := cmd_arb.io.out.valid && id_queue.io.enq.ready
    cmd_arb.io.out.ready := io.slave.cmd.ready && id_queue.io.enq.ready
    io.slave.cmd.bits    := cmd_arb.io.out.bits

    io.slave.rsp.ready := io.master(id_indic).rsp.ready && id_queue.io.deq.valid

  } else {
    io.slave <> io.master.head
  }
}


class IcbSplitterIO(splN: Int)(implicit p: Parameters) extends Bundle {
  val master = Flipped(new IcbIO)
  val slave  = Vec(splN, new IcbIO)
  val spl_id = Input(UInt(log2Ceil(splN + 1).W))
  override def cloneType =
    new IcbSplitterIO(splN).asInstanceOf[this.type]
}

/* split master to splN slaves */

class IcbSplitter(val splN: Int, val outN: Int = 1, val pipe: Boolean = true, val flow: Boolean = false)(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new IcbSplitterIO(splN))

  if (splN > 1) {
    val id_queue = Module(new Queue(chiselTypeOf(io.spl_id), entries = outN, pipe = pipe, flow = flow))

    id_queue.io.enq.bits := io.spl_id

    id_queue.io.enq.valid := io.master.cmd.fire
    id_queue.io.deq.ready := io.master.rsp.fire

    for (i <- 0 until splN) {
      io.slave(i).cmd.valid := io.master.cmd.valid && id_queue.io.enq.ready && (io.spl_id === i.asUInt)
      //io.slave(i).cmd.bits  := Mux(io.slave(i).cmd.valid, io.master.cmd.bits, 0.U.asTypeOf(chiselTypeOf(io.master.cmd.bits)))
      io.slave(i).cmd.bits  := io.master.cmd.bits
    }
    io.master.cmd.ready := io.slave(io.spl_id).cmd.ready && id_queue.io.enq.ready

    for (i <- 0 until splN) {
      io.slave(i).rsp.ready := io.master.rsp.ready && id_queue.io.deq.valid && (id_queue.io.deq.bits === i.asUInt)
    }
    io.master.rsp.valid := io.slave(id_queue.io.deq.bits).rsp.valid && id_queue.io.deq.valid
    //io.master.rsp.bits  := Mux(io.master.rsp.valid, io.slave(id_queue.io.deq.bits).rsp.bits, 0.U.asTypeOf(chiselTypeOf(io.master.rsp.bits)))
    io.master.rsp.bits  := io.slave(id_queue.io.deq.bits).rsp.bits

  } else {
    io.slave.head <> io.master
  }
}


class IcbBufferIO(implicit p: Parameters) extends Bundle {
  val master = Flipped(new IcbIO)
  val slave  = new IcbIO
  override def cloneType =
    new IcbBufferIO().asInstanceOf[this.type]
}

class IcbBuffer(val cmdDepth: Int = 1, val rspDepth: Int = 1, val cmdPipe: Boolean = true, val cmdFlow: Boolean = false, val rspPipe: Boolean = true, val rspFlow: Boolean = false)(implicit val p: Parameters) extends Module with HasIcbParameters {
  val io = IO(new IcbBufferIO)

  require(cmdDepth > -1, "Cmd Buffer must have non-negative number of entries")
  require(rspDepth > -1, "Rsp Buffer must have non-negative number of entries")

  if (cmdDepth > 0) {
    val cmd_queue = Module(new Queue(chiselTypeOf(io.master.cmd.bits), entries = cmdDepth, pipe = cmdPipe, flow = cmdFlow))

    cmd_queue.io.enq <> io.master.cmd
    io.slave.cmd <> cmd_queue.io.deq

  } else {
    io.slave.cmd <> io.master.cmd
  }

  if (rspDepth > 0) {
    val rsp_queue = Module(new Queue(chiselTypeOf(io.slave.rsp.bits), entries = rspDepth, pipe = rspPipe, flow = rspFlow))

    rsp_queue.io.enq <> io.slave.rsp
    io.master.rsp <> rsp_queue.io.deq

  } else {
    io.master.rsp <> io.slave.rsp
  }
}

