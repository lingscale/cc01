// See LICENSE for license details.

package lingscale.cc01.core

import java.io.{File, FileWriter}

object Main extends App {
//  val dir = new File(args(0)) ; dir.mkdirs
  val dir = new File("test_run_dir/Main") ; dir.mkdirs
  implicit val params = (new CoreConfig).toInstance
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Core()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new ALUArea()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new RegFile()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new RamMask(16, 16384)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new IcbArbiter(4)(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new IcbSpliter(4)(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new IcbBuffer(cmdDepth = 0, rspDepth = 0)(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Biu()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new CSR()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new ImmGen()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Oitf()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Datapath()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Dtcm()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Core()(params)))
  //val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Uart(0x10013000)))
  val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Soc()(params)))

  val writer = new FileWriter(new File(dir, s"${chirrtl.main}.fir"))
  writer write chirrtl.serialize
  writer.close

  val verilog = new FileWriter(new File(dir, s"${chirrtl.main}.v"))
  verilog write (new firrtl.VerilogCompiler).compileAndEmit(firrtl.CircuitState(chirrtl, firrtl.ChirrtlForm)).getEmittedCircuit.value
  verilog.close
}
