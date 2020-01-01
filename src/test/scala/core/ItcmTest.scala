// See README.md for license details.

package lingscale.cc01.core

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class ItcmUnitTester(c: Itcm) extends PeekPokeTester(c) {

  private val Itcm = c

  poke(Itcm.io.lsu.cmd.bits.read, 0)
  poke(Itcm.io.lsu.cmd.bits.wmask, 0)
  poke(Itcm.io.lsu.cmd.bits.wdata, 0)
  poke(Itcm.io.lsu.cmd.bits.addr, 0)
  poke(Itcm.io.lsu.cmd.valid, 0)
  poke(Itcm.io.lsu.rsp.ready, 0)

  poke(Itcm.io.ifu.cmd.bits.read, 0)
  poke(Itcm.io.ifu.cmd.bits.wmask, 0)
  poke(Itcm.io.ifu.cmd.bits.wdata, 0)
  poke(Itcm.io.ifu.cmd.bits.addr, 0)
  poke(Itcm.io.ifu.cmd.valid, 0)
  poke(Itcm.io.ifu.rsp.ready, 1)
  step(10)
  poke(Itcm.io.ifu.cmd.valid, 1)
  step(10)
  poke(Itcm.io.ifu.rsp.ready, 1)
  step(10)
  poke(Itcm.io.ifu.cmd.bits.read, 0)
  poke(Itcm.io.ifu.cmd.bits.wmask, 15)
  for (i <- 0 to 63) {
    step(1)
    poke(Itcm.io.ifu.cmd.bits.wdata, i + 5)
    poke(Itcm.io.ifu.cmd.bits.addr, i)
  }
  step(10)

  poke(Itcm.io.ifu.cmd.bits.read, 1)

  for (i <- 0 to 63) {
    step(1)
    poke(Itcm.io.ifu.cmd.bits.addr, i)
  }
  step(10)


}
