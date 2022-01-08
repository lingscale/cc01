// See LICENSE for license details.

package lingscale.cc01.core

import chisel3.stage.ChiselStage

object Main extends App {
  implicit val params = (new CoreConfig).toInstance
  //(new ChiselStage).emitVerilog((new Core()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new ALUArea()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new RegFile()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new RamMask(16, 16384)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new IcbArbiter(4)(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new IcbSpliter(4)(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new IcbBuffer(cmdDepth = 0, rspDepth = 0)(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Biu()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new CSR()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new ImmGen()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Oitf()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Datapath()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Dtcm()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Core()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Uart_fifo(8)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Uart(0x10013000)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Clint()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Plic()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Spi(0x10014000)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Ppi()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Soc()(params)), Array("--target-dir", "generatedsrc"))
  //(new ChiselStage).emitVerilog((new Soc()(params)), Array("--target-dir", "generatedsrc", "--no-dce"))
  //(new ChiselStage).emitVerilog((new Soc()(params)), Array("--target-dir", "generatedsrc", "--no-dce", "--no-cse", "--no-constant-propagation"))
  (new ChiselStage).emitVerilog((new Soc()(params)), Array("--target-dir", "generatedsrc", "--no-dce", "--no-cse", "--no-constant-propagation",
                                                           "--emission-options", "disableMemRandomization,disableRegisterRandomization"))
}
