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

class Tcm(tcm_ram_addr_width: Int, tcm_ram_depth: Int, allowCombLoopDet: Boolean)(implicit val p: Parameters) extends Module {
  val io = IO(Flipped(new IcbIO))

  val ram = Module(new RamMask(tcm_ram_addr_width, tcm_ram_depth))
//  val ram = Module(new Ram(tcm_ram_addr_width, tcm_ram_depth))  // use blackbox instantiate iCE40UP5K SPRAM
  
  ram.io.addr   := io.cmd.bits.addr
  ram.io.read   := io.cmd.bits.read && io.cmd.fire
  ram.io.dataIn := io.cmd.bits.wdata
  ram.io.mask   := io.cmd.bits.wmask
 
  if (allowCombLoopDet)
    ram.io.write  := !io.cmd.bits.read && io.cmd.fire  // comb loop for ifu. how to fix it?
  else
    ram.io.write  := !io.cmd.bits.read
  //  ram.io.write  := !io.cmd.bits.read && io.cmd.fire  // use blackbox instantiate iCE40UP5K SPRAM, no comb loop for ifu.

  io.rsp.bits.rdata := ram.io.dataOut
  io.rsp.bits.err   := false.B

  // ram data will be read out 1 cycle after read address, so valid-ready signal delay 1 stage
  // and remember, rsp back pressure cmd here. if rsp not accepted, no new cmd can issure.
  val tricky_queue = Module(new Queue(UInt(1.W), entries = 1, pipe = true, flow = false))

  tricky_queue.io.enq.bits := DontCare

  tricky_queue.io.enq.valid := io.cmd.valid
  io.cmd.ready := tricky_queue.io.enq.ready

  io.rsp.valid := tricky_queue.io.deq.valid
  tricky_queue.io.deq.ready := io.rsp.ready
}

class Dtcm(implicit val p: Parameters) extends Module with DtcmParams {
  val io = IO(Flipped(new IcbIO))

  val tcm = Module(new Tcm(dtcm_ram_addr_width, dtcm_ram_depth, true))

  io <> tcm.io
}

class Itcm(implicit val p: Parameters) extends Module with ItcmParams {
  val io = IO(new Bundle {
    val lsu = Flipped(new IcbIO)
    val ifu = Flipped(new IcbIO)
  })

  val arb = Module(new IcbArbiter(arbN = 2, outN = 1, pipe = true, flow = false))
  val tcm = Module(new Tcm(itcm_ram_addr_width, itcm_ram_depth, false))

  io.lsu <> arb.io.master(0)  // lsu has higher priotry than the ifetch unit
  io.ifu <> arb.io.master(1)
  arb.io.slave <> tcm.io
}

