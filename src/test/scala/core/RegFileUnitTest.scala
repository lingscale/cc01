// See README.md for license details.

package lingscale.cc01.core

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class RegFileUnitTester(c: RegFile) extends PeekPokeTester(c) {

//  implicit val p = (new CoreConfig).toInstance
  private val RegFile = c

  poke(RegFile.io.rdwen, 1)
  for (i <- 0 to 31) {
    step(1)
    poke(RegFile.io.rdidx, i)
    poke(RegFile.io.rd, i + 5)
  }

  step(1)
  poke(RegFile.io.rdwen, 0)

  for (i <- 0 to 31) {
    step(1)
    poke(RegFile.io.rs1idx, i)
    expect(RegFile.io.rs1, i + 5)
  }
}
