// See README.md for license details.

package lingscale.cc01.core

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class RamMaskUnitTester(c: RamMask) extends PeekPokeTester(c) {

  private val RamMask = c

  poke(RamMask.io.write, 0)
  poke(RamMask.io.read, 0)
  step(10)
  poke(RamMask.io.write, 1)
  poke(RamMask.io.mask, 1)
  step(1)
  poke(RamMask.io.mask, 3)
  step(1)
  poke(RamMask.io.mask, 7)
  step(1)
  poke(RamMask.io.mask, 15)


  for (i <- 0 to 63) {
    step(1)
    poke(RamMask.io.addr, i)
    poke(RamMask.io.dataIn, i + 1073741824)
  }

  step(1)
  poke(RamMask.io.write, 0)
  step(2)
  poke(RamMask.io.read, 1)
    poke(RamMask.io.mask, 0)
  for (i <- 0 to 63) {
    step(1)
    poke(RamMask.io.addr, i)
    step(1)
    expect(RamMask.io.dataOut, i + 1073741824)
  }

  step(10)
}
