// See README.md for license details.

package lingscale.cc01.core

import chisel3._

/**
  * This provides an alternate way to run tests, by executing then as a main
  * From sbt (Note: the test: prefix is because this main is under the test package hierarchy):
  * {{{
  * test:runMain gcd.GCDMain
  * }}}
  * To see all command line options use:
  * {{{
  * test:runMain gcd.GCDMain --help
  * }}}
  * To run with verilator:
  * {{{
  * test:runMain lingscale.cc01.core.RamMaskMain --backend-name verilator
  * }}}
  * To run with verilator from your terminal shell use:
  * {{{
  * sbt 'test:runMain gcd.GCDMain --backend-name verilator'
  * }}}
  */
object RamMaskMain extends App {
//  implicit val p = (new CoreConfig).toInstance
  iotesters.Driver.execute(args, () => new RamMask(16, 16384)) {
    c => new RamMaskUnitTester(c)
  }
}
