package lingscale.cc01.core

import chisel3._
import chisel3.util.Queue
import scala.math.max
import scala.collection.mutable.ArraySeq
import lingscale.cc01.config.{Field, Parameters}

case object DTCM_RAM_WIDTH extends Field[Int]
case object DTCM_RAM_ADDR_WIDTH extends Field[Int]
case object DTCM_RAM_DEPTH extends Field[Int]

case object ITCM_RAM_WIDTH extends Field[Int]
case object ITCM_RAM_ADDR_WIDTH extends Field[Int]
case object ITCM_RAM_DEPTH extends Field[Int]

trait DtcmParams {
  implicit val p: Parameters
  val dtcm_ram_addr_width = p(DTCM_RAM_ADDR_WIDTH)
  val dtcm_ram_depth = p(DTCM_RAM_DEPTH)
}

trait ItcmParams {
  implicit val p: Parameters
  val itcm_ram_addr_width = p(ITCM_RAM_ADDR_WIDTH)
  val itcm_ram_depth = p(ITCM_RAM_DEPTH)
}

class Tcm(tcm_ram_addr_width: Int, tcm_ram_depth: Int)(implicit val p: Parameters) extends Module {
  val io = IO(Flipped(new IcbIO))

//  val ram = Module(new RamMask(tcm_ram_addr_width, tcm_ram_depth))  // for simulation and LFE5U-45F
  val ram = Module(new Ram(tcm_ram_addr_width, tcm_ram_depth))  // use blackbox instantiate iCE40UP5K SPRAM

  // bypass cmd channel to cut down the back-pressure ready signal
//  val icb_buffer = Module(new IcbBuffer(cmdDepth = 1, rspDepth = 0, cmdPipe = false, cmdFlow = true, rspPipe = true, rspFlow = false))
  val icb_buffer = Module(new IcbBuffer(cmdDepth = 0, rspDepth = 0, cmdPipe = false, cmdFlow = true, rspPipe = true, rspFlow = false))
  io <> icb_buffer.io.master

  ram.io.addr   := icb_buffer.io.slave.cmd.bits.addr
  ram.io.write  := !icb_buffer.io.slave.cmd.bits.read
  ram.io.dataIn := icb_buffer.io.slave.cmd.bits.wdata
  ram.io.mask   := icb_buffer.io.slave.cmd.bits.wmask
  ram.io.enable := icb_buffer.io.slave.cmd.fire
 
  icb_buffer.io.slave.rsp.bits.rdata := ram.io.dataOut
  icb_buffer.io.slave.rsp.bits.err   := false.B

  // ram data will be read out 1 cycle after read address, so valid-ready signal delay 1 stage
  // and remember, rsp back pressure cmd here. if rsp not accepted, no new cmd can issure.
  val tricky_queue = Module(new Queue(UInt(1.W), entries = 1, pipe = true, flow = false))

  tricky_queue.io.enq.bits := DontCare

  tricky_queue.io.enq.valid := icb_buffer.io.slave.cmd.valid
  icb_buffer.io.slave.cmd.ready := tricky_queue.io.enq.ready

  icb_buffer.io.slave.rsp.valid := tricky_queue.io.deq.valid
  tricky_queue.io.deq.ready := icb_buffer.io.slave.rsp.ready
}

class Dtcm(implicit val p: Parameters) extends Module with DtcmParams {
  val io = IO(Flipped(new IcbIO))

  val tcm = Module(new Tcm(dtcm_ram_addr_width, dtcm_ram_depth))

  io <> tcm.io
}

class Itcm(implicit val p: Parameters) extends Module with ItcmParams {
  val io = IO(new Bundle {
    val lsu = Flipped(new IcbIO)
    val ifu = Flipped(new IcbIO)
  })

  val arb = Module(new IcbArbiter(arbN = 2, outN = 1, pipe = true, flow = false))
  val tcm = Module(new Tcm(itcm_ram_addr_width, itcm_ram_depth))

  io.lsu <> arb.io.master(0)  // lsu has higher priotry than the ifetch unit
  io.ifu <> arb.io.master(1)
  arb.io.slave <> tcm.io
}

