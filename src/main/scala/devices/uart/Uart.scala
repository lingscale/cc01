package lingscale.cc01.core

import chisel3._
import chisel3.util._
import lingscale.cc01.config.{Field, Parameters}

// Universal Asynchronous Receiver/Transmitter with features:
// 8-N-1 and 8-N2 formats
// 8-entry transmitter fifo and receiver fifo with watermark interrupts
class Uart(base_addr: Int)(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new Bundle{
    val icb = Flipped(new IcbIO)
    val tx = Output(Bool())
    val rx = Input(Bool())
    val uart_irq = Output(Bool())
  })

  val txdata_addr = (base_addr + 0x00).asUInt  // transmit data register
  val rxdata_addr = (base_addr + 0x04).asUInt  // receive data register
  val txctrl_addr = (base_addr + 0x08).asUInt  // transmit control register
  val rxctrl_addr = (base_addr + 0x0c).asUInt  // receive control register
  val ie_addr     = (base_addr + 0x10).asUInt  // interrupt enable
  val ip_addr     = (base_addr + 0x14).asUInt  // interrupt pending
  val div_addr    = (base_addr + 0x18).asUInt  // baud rate divisor

  val txfifo = Module(new Uart_fifo())
  val txfifo_write = Wire(Bool())
  val txfifo_read = RegInit(false.B)
  txfifo.io.write := txfifo_write
  txfifo.io.read := txfifo_read

  io.icb.cmd.ready := true.B
  io.icb.rsp.valid := true.B
  io.icb.rsp.bits.rdata := 0.U
  io.icb.rsp.bits.err := false.B

  val rxfifo = Module(new Uart_fifo())
  val rxfifo_write = RegInit(false.B)
  val rxfifo_read = Wire(Bool())
  rxfifo.io.write := rxfifo_write
  rxfifo.io.read := rxfifo_read

  val txdata_data = io.icb.cmd.bits.wdata(7, 0)  // transmit data
  val txdata_full = txfifo.io.full               // transmit fifo full
  //val txdata = Cat(txdata_full, 0.U(23.W), txdata_data)
  txfifo.io.in := txdata_data

  txfifo_write :=io.icb.cmd.fire && (io.icb.cmd.bits.addr === txdata_addr) && !io.icb.cmd.bits.read

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

  val txctrl_txen = RegInit(false.B)    // transmit enable
  val txctrl_nstop = RegInit(false.B)   // number fo stop bits, 0 1bit, 1 2bits
  val txctrl_txcnt = RegInit(0.U(3.W))  // tanasmit watermark level
  val txctrl = Cat(0.U(13.W), txctrl_txcnt, 0.U(14.W), txctrl_nstop, txctrl_txen)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txctrl_addr) && !io.icb.cmd.bits.read) {
    txctrl_txen := io.icb.cmd.bits.wdata(0)
    txctrl_nstop := io.icb.cmd.bits.wdata(1)
    txctrl_txcnt := io.icb.cmd.bits.wdata(18, 16)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === txctrl_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := txctrl
  }

  val rxctrl_rxen = RegInit(false.B)    // receive enable
  val rxctrl_rxcnt = RegInit(0.U(3.W))  // receive watermark level
  val rxctrl = Cat(0.U(13.W), rxctrl_rxcnt, 0.U(15.W), rxctrl_rxen)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === rxctrl_addr) && !io.icb.cmd.bits.read) {
    rxctrl_rxen := io.icb.cmd.bits.wdata(0)
    rxctrl_rxcnt := io.icb.cmd.bits.wdata(18, 16)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === rxctrl_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := rxctrl
  }

  val ie_txwm = RegInit(false.B)  // transmit watermark interrupt enbale
  val ie_rxwm = RegInit(false.B)  // receive watermark interrupt enbale
  val ie = Cat(0.U(30.W), ie_rxwm, ie_txwm)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ie_addr) && !io.icb.cmd.bits.read) {
    ie_txwm := io.icb.cmd.bits.wdata(0)
    ie_rxwm := io.icb.cmd.bits.wdata(1)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ie_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := ie
  }

  val ip_txwm = txfifo.io.number < txctrl_txcnt  // transmit watermark interrupt pending
  val ip_rxwm = rxfifo.io.number > rxctrl_rxcnt  // receive watermark interrupt pending
  val ip = Cat(0.U(30.W), ip_rxwm, ip_txwm)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === ip_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := ip
  }

  val div_div = Reg(UInt(16.W))  // baud rate divesor. 11.2896M / ( 195 + 1 ) = 57600, 0.0% error to 57600; 11.2896M / ( 97 + 1 ) = 115200, 0.0% error to 115200.
  val div = Cat(0.U(16.W), div_div)

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === div_addr) && !io.icb.cmd.bits.read) {
    div_div := io.icb.cmd.bits.wdata(15, 0)
  }

  when (io.icb.cmd.fire && (io.icb.cmd.bits.addr === div_addr) && io.icb.cmd.bits.read) {
    io.icb.rsp.bits.rdata := div
  }

  // tx function

  val tx_counter = Reg(UInt(16.W))
  when (txctrl_txen && !txfifo.io.empty) { tx_counter := tx_counter + 1.U } .otherwise { tx_counter := 0.U }
  when (tx_counter === div_div) { tx_counter := 0.U }

  val tx = RegInit(true.B)
  val tx_frame = Cat(txfifo.io.out, 0.U(1.W))

  val tx_ptr = RegInit(0.U(4.W))

  when (!txfifo.io.empty && tx_counter === div_div) {
    when (tx_ptr < 9.U) {
      tx := tx_frame(tx_ptr)
      tx_ptr := tx_ptr + 1.U
    }
    .elsewhen (tx_ptr === 9.U) {
      tx := true.B
      switch (txctrl_nstop) {
        is (true.B) {
          tx_ptr := tx_ptr + 1.U
        }
        is (false.B) {
          txfifo_read := true.B
          tx_ptr := 0.U
        }
      }
    }
    .otherwise {
      txfifo_read := true.B
      tx_ptr := 0.U
    }
  }

  when (txfifo_read) { txfifo_read := false.B }

  // rx function

  val rx = io.rx

  val rxbuf = Reg(Vec(8, Bool()))
  rxfifo.io.in := rxbuf.asUInt
  val rxbuf_ptr = Reg(UInt(4.W))

  val rx_counter = Reg(UInt(16.W))
  val rx_counter_en = RegInit(false.B)
  when (rx_counter_en) { rx_counter := rx_counter + 1.U } .otherwise { rx_counter := 0.U }
  when (rx_counter === div_div) { rx_counter := 0.U }

  val s_IDLE :: s_START :: s_DATA :: s_STOP :: s_WRITE :: Nil = Enum(5)
  val state = RegInit(s_IDLE)

  switch (state) {
    is (s_IDLE) {
      when (rxctrl_rxen && !rx) {
        rx_counter_en := true.B
        state := s_START
      }
      .otherwise {
        rx_counter_en := false.B
        state := s_IDLE
      }
      rxbuf_ptr := 0.U
    }
    is (s_START) {
      when (rxctrl_rxen && rx_counter === div_div >> 1 && !rx) {
        state := s_DATA
      }
      .elsewhen (rxctrl_rxen && !rx) {
        state := s_START
      }
      .otherwise {
        state := s_IDLE
      }
    }
    is (s_DATA) {
      when (rxctrl_rxen && rx_counter === div_div >> 1) {
        when (rxbuf_ptr <= 7.U) {
          rxbuf(rxbuf_ptr) := rx
          rxbuf_ptr := rxbuf_ptr + 1.U
          state := s_DATA
        }
        .otherwise {
          state := s_STOP
        }
      }
      .elsewhen (rxctrl_rxen) {
        state := s_DATA
      }
      .otherwise {
        state := s_IDLE
      }
    }
    is (s_STOP) {
      when (rxctrl_rxen && !rxfifo.io.full) {
        rxfifo_write := true.B
        state := s_IDLE
      }
      .otherwise {
        state := s_IDLE
      }
    }
  }

  when (rxfifo_write) { rxfifo_write := false.B }

  io.tx := tx

  io.uart_irq := (ie_txwm && ip_txwm) || (ie_rxwm && ip_rxwm)
}
