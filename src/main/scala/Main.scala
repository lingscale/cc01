// See LICENSE for license details.

package lingscale.cc01.core

import circt.stage.ChiselStage
import lingscale.cc01.config.Config

object Main extends App {
  implicit val params: Config = (new CoreConfig).toInstance

  val emitargs: Array[String] = Array(
    "--target-dir",
    "generatedsrc"
  )

  val firtoolOpts: Array[String] = Array(
    "--no-dedup",
    "--disable-all-randomization",
    "--disable-opt",
    "--verification-flavor=sva",
    "--preserve-values=named",
    "--strip-debug-info",
    "--strip-fir-debug-info"
  )

  ChiselStage.emitSystemVerilogFile(gen = new Soc()(params), args = emitargs, firtoolOpts = firtoolOpts)
}
