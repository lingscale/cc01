package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

// Serial Peripheral Interface(SPI)
// Now only support winbond standard spi Read Data (03h) instruction.
// 8-entry transmitter fifo and receiver fifo with watermark interrupts
class Spi(base_addr: Int = 0x10014000)(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new Bundle{
    val icb  = Flipped(new IcbIO)
    val MOSI = Output(Bool())
    val MISO = Input(Bool())
    val SS   = Output(Bool())
    val SCK  = Output(Bool())
  })

  val sckdiv_addr  = (base_addr + 0x00).asUInt  // serial clock divisor
  val sckmode_addr = (base_addr + 0x04).asUInt  // serial clock mode
  val csid_addr    = (base_addr + 0x10).asUInt  // chip select ID
  val csdef_addr   = (base_addr + 0x14).asUInt  // chip select default
  val csmode_addr  = (base_addr + 0x18).asUInt  // chip select mode
  val delay0_addr  = (base_addr + 0x28).asUInt  // delay control 0
  val delay1_addr  = (base_addr + 0x2c).asUInt  // delay control 1
  val fmt_addr     = (base_addr + 0x40).asUInt  // frame format
  val txdata_addr  = (base_addr + 0x48).asUInt  // Tx FIFO data
  val rxdata_addr  = (base_addr + 0x4c).asUInt  // Rx FIFO data
  val txmark_addr  = (base_addr + 0x50).asUInt  // Tx FIFO watermark
  val rxmark_addr  = (base_addr + 0x54).asUInt  // Rx FIFO watermark
  val fctrl_addr   = (base_addr + 0x60).asUInt  // spi flash interfact control
  val ffmt_addr    = (base_addr + 0x64).asUInt  // spi flash instruction format
  val ie_addr      = (base_addr + 0x70).asUInt  // spi interrupt enable
  val ip_addr      = (base_addr + 0x74).asUInt  // spi interrupt pending

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U(xlen.W)
  io.icb.rsp.bits.err := false.B

  val txfifo = Module(new Spi_fifo())
  val txfifo_write = RegInit(false.B)
  val txfifo_read = RegInit(false.B)
  txfifo.io.write := txfifo_write
  txfifo.io.read := txfifo_read

  val rxfifo = Module(new Spi_fifo())
  val rxfifo_write = RegInit(false.B)
  val rxfifo_read = RegInit(false.B)
  rxfifo.io.write := rxfifo_write
  rxfifo.io.read := rxfifo_read

  val txdata_data = io.icb.cmd.bits.wdata(7, 0)  // transmit data
  val txdata_full = txfifo.io.full               // transmit fifo full
  //val txdata = Cat(txdata_full, 0.U(23.W), txdata_data)  // Transmit Data Register (txdata)
  txfifo.io.in := txdata_data

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txdata_addr) && !io.icb.cmd.bits.read) {
    txfifo_write := true.B
  }
  .otherwise {
    txfifo_write := false.B
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txdata_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(txdata_full, 0.U(31.W))
  }

  val rxdata_data = rxfifo.io.out     // received data
  val rxdata_empty = rxfifo.io.empty  // receive fifo empty
  val rxdata = Cat(rxdata_empty, 0.U(23.W), rxdata_data)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === rxdata_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := rxdata
    rxfifo_read := true.B
  } .otherwise {
    rxfifo_read := false.B
  }

/*
  val div = RegInit(3.U(12.W))  // Divisor for serial clock. div_width bits wide.
  val sckdiv = Cat(0.U(20.W), div)  // Serial Clock Divisor Register (sckdiv)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === sckdiv_addr) && !io.icb.cmd.bits.read) {
    div := io.icb.cmd.bits.wdata(11, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === sckdiv_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := sckdiv
  }

  val pha = RegInit(false.B)  // Serial clock phase
  val pol = RegInit(false.B)  // Serial clock polarity
  val sckmode = Cat(0.U(30.W), pol, pha)  // Serial Clock Mode Register (sckmode)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === sckmode_addr) && !io.icb.cmd.bits.read) {
    pha := io.icb.cmd.bits.wdata(0)
    pol := io.icb.cmd.bits.wdata(1)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === sckmode_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := sckmode
  }

  val csid = RegInit(0.U(32.W))  // Chip Select ID Register (csid)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csid_addr) && !io.icb.cmd.bits.read) {
    csid := io.icb.cmd.bits.wdata
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csid_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := csid
  }
*/
  val csdef = RegInit("hffff_ffff".U(32.W))  // Chip Select Default Register (csdef)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csdef_addr) && !io.icb.cmd.bits.read) {
    csdef := io.icb.cmd.bits.wdata
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csdef_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := csdef
  }
/*
  val mode = RegInit(0.U(2.W))  // Chip Select Mode
  val csmode = Cat(0.U(30.W), mode)  // Chip Select Mode Register (csmode)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csmode_addr) && !io.icb.cmd.bits.read) {
    mode := io.icb.cmd.bits.wdata(1, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === csmode_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := csmode
  }
*/
  val cssck = RegInit(1.U(8.W))  // CS to SCK Delay
  val sckcs = RegInit(1.U(8.W))  // SCK to CS Delay
  val delay0 = Cat(0.U(8.W), sckcs, 0.U(8.W), cssck)  // Delay Control Register 0 (delay0)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === delay0_addr) && !io.icb.cmd.bits.read) {
    cssck := io.icb.cmd.bits.wdata(7, 0)
    sckcs := io.icb.cmd.bits.wdata(23, 16)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === delay0_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := delay0
  }
/*
  val intercs = RegInit(1.U(8.W))  // Minimum CS inacctive time
  val interxfr = RegInit(0.U(8.W))  // Maximum interframe delay
  val delay1 = Cat(0.U(8.W), interxfr, 0.U(8.W), intercs)  // Delay Control Register 1 (delay1)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === delay1_addr) && !io.icb.cmd.bits.read) {
    intercs := io.icb.cmd.bits.wdata(7, 0)
    interxfr := io.icb.cmd.bits.wdata(23, 16)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === delay1_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := delay1
  }

  val proto = RegInit(0.U(2.W))  // SPI protocol
  val endian = RegInit(false.B)  // SPI endianness
  val dir = RegInit(false.B)  // SPI I/O direction. This is reset to 1 for flash-enabled SPI controllers, 0 otherwise.
  val len = RegInit(8.U(4.W))  // Number of bits per frame
  val fmt = Cat(0.U(12.W), len, 0.U(12.W), dir, endian, proto)  // Delay Control Register 1 (delay1)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === fmt_addr) && !io.icb.cmd.bits.read) {
    proto := io.icb.cmd.bits.wdata(1, 0)
    endian := io.icb.cmd.bits.wdata(2)
    dir := io.icb.cmd.bits.wdata(3)
    len := io.icb.cmd.bits.wdata(19, 16)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === fmt_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := fmt
  }

  val txmark = RegInit(0.U(3.W))  // Transmit watermark. The reset value is 1 for flash-enabled controllers, 0 otherwise.
  //val txmark = Cat(0.U(29.W), txmark) // Transmit Watermark Register (txmark)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txmark_addr) && !io.icb.cmd.bits.read) {
    txmark := io.icb.cmd.bits.wdata(2, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txmark_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), txmark)
  }

  val rxmark = RegInit(0.U(3.W))  // Receive watermark.
  //val rxmark = Cat(0.U(29.W), rxmark) // Receive Watermark Register (rxmark)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === rxmark_addr) && !io.icb.cmd.bits.read) {
    rxmark := io.icb.cmd.bits.wdata(2, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === rxmark_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := Cat(0.U(29.W), rxmark)
  }

  val ie_txwm = RegInit(false.B)  // Transmit watermark enable
  val ie_rxwm = RegInit(false.B)  // Receive watermark enable
  val ie = Cat(0.U(30.W), ie_rxwm, ie_txwm)  // SPI Interrupt Enable Register

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ie_addr) && !io.icb.cmd.bits.read) {
    ie_txwm := io.icb.cmd.bits.wdata(0)
    ie_rxwm := io.icb.cmd.bits.wdata(1)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ie_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := ie
  }

  val ip_txwm = txfifo.io.number < txmark  // Transmit watermark pending
  val ip_rxwm = rxfifo.io.number < rxmark  // Receive watermark pending
  val ip = Cat(0.U(30.W), ip_rxwm, ip_txwm)  // SPI Watermark Interrupt Pending Register

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ip_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := ip
  }

  val fctrl_en = RegInit(true.B)  // SPI Flash Mode Select
  val fctrl = Cat(0.U(31.W), fctrl_en)  // SPI Flash Interface Control Register (fctrl)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === fctrl_addr) && !io.icb.cmd.bits.read) {
    fctrl_en := io.icb.cmd.bits.wdata(0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === fctrl_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := fctrl
  }

  val cmd_en     = RegInit(true.B)    // Enable sending of command
  val addr_len   = RegInit(3.U(3.W))  // Number of address bytes (0 to 4)
  val pad_cnt    = RegInit(0.U(4.W))  // Number of dummy bytes
  val cmd_proto  = RegInit(0.U(2.W))  // Portocol for transmitting command
  val addr_proto = RegInit(0.U(2.W))  // Portocol for transmitting address and padding
  val data_proto = RegInit(0.U(2.W))  // Portocol for receiving data bytes
  val cmd_code   = RegInit(3.U(8.W))  // Value of command byte
  val pad_code   = RegInit(0.U(8.W))  // First 8 bits to transmit during dummy cycles
  val ffmt = Cat(pad_code, cmd_code, 0.U(2.W), data_proto, addr_proto, cmd_proto, pad_cnt, addr_len, cmd_en)  // SPI Flash Instruction Format Register

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ffmt_addr) && !io.icb.cmd.bits.read) {
    cmd_en     := io.icb.cmd.bits.wdata(0)
    addr_len   := io.icb.cmd.bits.wdata(3, 1)
    pad_cnt    := io.icb.cmd.bits.wdata(7, 4)
    addr_proto := io.icb.cmd.bits.wdata(11, 10)
    data_proto := io.icb.cmd.bits.wdata(13, 12)
    cmd_code   := io.icb.cmd.bits.wdata(23, 16)
    pad_code   := io.icb.cmd.bits.wdata(31, 24)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ffmt_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := ffmt
  }
*/

  val rxbuf = Reg(Vec(8, Bool()))
  rxfifo.io.in := rxbuf.asUInt

  val ptr = RegInit(7.U(3.W))
  val MOSI = txfifo.io.out(ptr)
  val MISO = io.MISO
  val SS   = RegInit(true.B)
  val SCK  = RegInit(false.B)
  io.MOSI := MOSI
  io.SS := SS
  io.SCK := SCK


  val s_IDLE :: s_CSSCK :: s_TXDATA :: s_RXDATA :: s_SCKCS :: Nil = Enum(5)
  val s_state = RegInit(s_IDLE)
  val count = Reg(UInt(8.W))

  switch (s_state) {
    is (s_IDLE) {
      when (csdef === "hffff_ffff".U) {
        SS := true.B
      }
      .elsewhen (cssck =/= 0.U ) {
        SS := false.B
        count := cssck - 1.U
        s_state := s_CSSCK
      }
      .otherwise {
        SS := false.B
        s_state := s_TXDATA
      }
    }
    is (s_CSSCK) {
      when (count =/= 0.U) {
        count := count - 1.U
        s_state := s_CSSCK
      }
      .otherwise {
        s_state := s_TXDATA
      }
    }
    is (s_TXDATA) {
      when (txfifo.io.empty) {
        txfifo_read := false.B
        s_state := s_RXDATA
      }
      .elsewhen (!SCK) {
        SCK := true.B
        txfifo_read := false.B
      }
      .elsewhen (ptr =/= 0.U) {
        SCK := false.B
        ptr := ptr - 1.U
      }
      .elsewhen (txfifo.io.number === 1.U) {
        SCK := false.B
        txfifo_read := true.B
        ptr := 7.U
        s_state := s_RXDATA
      }
      .otherwise {
        SCK := false.B
        txfifo_read := true.B
        ptr := 7.U
      }
    }
    is (s_RXDATA) {
      when (csdef === "hffff_ffff".U) {
        SCK := false.B
        txfifo_read := false.B
        rxfifo_write := false.B
        ptr := 0.U
        s_state := s_SCKCS
      }
      .elsewhen (rxfifo.io.full) {
      }
      .elsewhen (!SCK) {
        SCK := true.B
        txfifo_read := false.B
        rxbuf(ptr) := MISO
        rxfifo_write := false.B
      }
      .elsewhen (ptr =/= 0.U) {
        SCK := false.B
        ptr := ptr - 1.U
      }
      .otherwise {
        SCK := false.B
        rxfifo_write := true.B
        ptr := 7.U
      }
    }
    is (s_SCKCS) {
      when (count =/= 0.U) {
        count := count - 1.U
        s_state := s_SCKCS
      }
      .otherwise {
        SS := true.B
        SCK := false.B
        s_state := s_IDLE
      }
    }
  }

}



