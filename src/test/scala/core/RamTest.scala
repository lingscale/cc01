// See README.md for license details.

package lingscale.cc01.core

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class RamUnitTester(c: Ram) extends PeekPokeTester(c) {

  private val Ram = c

  poke(Ram.io.write, 0)
  poke(Ram.io.read, 0)
  step(10)
  poke(Ram.io.write, 1)


  for (i <- 0 to 63) {
    step(1)
    poke(Ram.io.addr, i)
    poke(Ram.io.dataIn, i + 5)
  }

  step(1)
  poke(Ram.io.write, 0)
  step(2)
  poke(Ram.io.read, 1)

  for (i <- 0 to 63) {
    step(1)
    poke(Ram.io.addr, i)
    step(1)
    expect(Ram.io.dataOut, i + 5)
  }
}
