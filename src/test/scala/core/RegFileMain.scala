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
  * test:runMain gcd.GCDMain --generate-vcd-output on
  * test:runMain gcd.GCDMain --generate-vcd-output off
  * To run with verilator:
  * {{{
  * test:runMain lingscale.cc01.core.RegFileMain --backend-name verilator
  * }}}
  * To run with verilator from your terminal shell use:
  * {{{
  * sbt 'test:runMain gcd.GCDMain --backend-name verilator'
  * }}}
  */
object RegFileMain extends App {
  implicit val p = (new CoreConfig).toInstance
  iotesters.Driver.execute(args, () => new RegFile) {
    c => new RegFileUnitTester(c)
  }
}
