// See README.md for license details.

package lingscale.cc01.core

import chisel3._

/**
  * This provides an alternate way to run tests, by executing then as a main
  * From sbt (Note: the test: prefix is because this main is under the test package hierarchy):
  * {{{
  * test:runMain lingscale.cc01.core.SocMain
  * test:runMain lingscale.cc01.core.SocMain --generate-vcd-output on
  * test:runMain lingscale.cc01.core.SocMain --generate-vcd-output off
  * }}}
  * To see all command line options use:
  * {{{
  * test:runMain lingscale.cc01.core.SocMain --help
  * }}}
  * test:runMain lingscale.cc01.core.SocMain --generate-vcd-output on
  * To run with verilator:
  * {{{
  * test:runMain lingscale.cc01.core.SocMain --backend-name verilator
  * }}}
  * To run with verilator from your terminal shell use:
  * {{{
  * sbt 'test:runMain lingscale.cc01.core.SocMain --backend-name verilator'
  * }}}
  */
object SocMain extends App {
  implicit val p = (new CoreConfig).toInstance
  iotesters.Driver.execute(args, () => new Soc) {
    c => new SocUnitTester(c)
  }
}
