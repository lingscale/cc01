package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

class BlackBoxRom extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle{
    val q = Output(UInt(32.W))
    val addr = Input(UInt(32.W))
    val read = Input(Bool())
    val clk = Input(Clock())
  })
  addResource("/BlackBoxRom.v")
}

class Rom(implicit val p: Parameters) extends Module {
  val io = IO(Flipped(new IcbIO))

  val ram = Module(new BlackBoxRom)

  ram.io.addr := io.cmd.bits.addr
  ram.io.read := io.cmd.bits.read && io.cmd.fire
  ram.io.clk := clock

  io.rsp.bits.rdata := ram.io.q
  io.rsp.bits.err := false.B

  // ram data will be read out 1 cycle after read address, so valid-ready signal delay 1 stage
  // and remember, rsp back pressure cmd here. if rsp not accepted, no new cmd can issure.
  val tricky_queue = Module(new Queue(UInt(1.W), entries = 1, pipe = false, flow = false)) 
  //val tricky_queue = Module(new Queue(UInt(1.W), entries = 1, pipe = true, flow = false)) 

  tricky_queue.io.enq.bits := DontCare

  tricky_queue.io.enq.valid := io.cmd.valid
  io.cmd.ready := tricky_queue.io.enq.ready

  io.rsp.valid := tricky_queue.io.deq.valid
  tricky_queue.io.deq.ready := io.rsp.ready

}
