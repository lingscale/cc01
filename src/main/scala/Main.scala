// See LICENSE for license details.

package lingscale.cc01.core

import circt.stage.ChiselStage

object Main extends App {
  implicit val params = (new CoreConfig).toInstance

  val emitargs: Array[String] = Array(
    "--target-dir",
    "generatedsrc"
  )

  val firtoolOpts: Array[String] = Array(
    "--dedup",
    "--disable-all-randomization",
    "--disable-opt",
    "--emit-chisel-asserts-as-sva",
    "--preserve-values=named",
    "--strip-debug-info",
    "--strip-fir-debug-info"
  )

  ChiselStage.emitSystemVerilogFile(gen = new Soc()(params), args = emitargs, firtoolOpts = firtoolOpts)
}
