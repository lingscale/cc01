// See README.md for license details.

package lingscale.cc01.core

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class SocUnitTester(c: Soc) extends PeekPokeTester(c) {

  private val Soc = c

  //step(64)
  //step(1800)
  //step(6800)
  //step(40000)
  //step(80000)
  step(700000)
}
