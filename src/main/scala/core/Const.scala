package lingscale.cc01.core

import chisel3._
import chisel3.util.{BitPat}

object Const {
//  def PC_RTVEC = "h80000000"
//  def PC_TVEC  = "h80000004"
  def PC_RTVEC = "h20000000"
//  def PC_TVEC  = "h20000004"
  def PC_TVEC  = "h80000004"
}

object AddrRegion {
  def ITCM_ADDR  = BitPat("b10000000_00000000_????????_????????")  // 0x8000_0000 ~ 0x8000_FFFF   64KByte
  def DTCM_ADDR  = BitPat("b10010000_00000000_????????_????????")  // 0x9000_0000 ~ 0x9000_FFFF   64KByte
//  def ITCM_ADDR  = BitPat("b10000000_00000000_000000??_????????")  // 0x8000_0000 ~ 0x8000_03FF   1KByte
//  def DTCM_ADDR  = BitPat("b10010000_00000000_000000??_????????")  // 0x9000_0000 ~ 0x9000_03FF   1KByte
  def MEM_ADDR   = BitPat("b001?????_????????_????????_????????")  // 0x2000_0000 ~ 0x3FFF_FFFF
  def PPI_ADDR   = BitPat("b0001????_????????_????????_????????")  // 0x1000_0000 ~ 0x1FFF_FFFF
//def FIO_ADDR   = BitPat("b1111????_????????_????????_????????")  // 0xF000_0000 ~ 0xFFFF_FFFF
  def CLINT_ADDR = BitPat("b00000010_00000000_????????_????????")  // 0x0200_0000 ~ 0x0200_FFFF
  def PLIC_ADDR  = BitPat("b00001100_????????_????????_????????")  // 0x0C00_0000 ~ 0x0CFF_FFFF
}

object PpiRegion {
  def UART_ADDR = BitPat("b00010000_00000001_00110000_????????")  // 0x1001_3000 ~ 0x1001_30FF
  def SPI_ADDR  = BitPat("b00010000_00000001_01000000_????????")  // 0x1001_4000 ~ 0x1001_40FF
}

object UartAddr {
  def UART_BASE_ADDR = 0x10013000
  def TXDATA_ADDR = UART_BASE_ADDR + 0x00  // Transmit data register
  def RXDATA_ADDR = UART_BASE_ADDR + 0x04  // Receive data register
  def TXCTRL_ADDR = UART_BASE_ADDR + 0x08  // Transmit control register
  def RXCTRL_ADDR = UART_BASE_ADDR + 0x0C  // Receive control register
  def IE_ADDR     = UART_BASE_ADDR + 0x10  // UART interrupt enable
  def IP_ADDR     = UART_BASE_ADDR + 0x14  // UART interrupt pending
  def DIV_ADDR    = UART_BASE_ADDR + 0x18  // Baud rate divisor
}

object SpiAddr {
  def SPI_BASE_ADDR = 0x10014000
  val SCKDIV_ADDR  = SPI_BASE_ADDR + 0x00  // serial clock divisor
  val SCKMODE_ADDR = SPI_BASE_ADDR + 0x04  // serial clock mode
  val CSID_ADDR    = SPI_BASE_ADDR + 0x10  // chip select ID
  val CSDEF_ADDR   = SPI_BASE_ADDR + 0x14  // chip select default
  val CSMODE_ADDR  = SPI_BASE_ADDR + 0x18  // chip select mode
  val DELAY0_ADDR  = SPI_BASE_ADDR + 0x28  // delay control 0
  val DELAY1_ADDR  = SPI_BASE_ADDR + 0x2C  // delay control 1
  val FMT_ADDR     = SPI_BASE_ADDR + 0x40  // frame format
  val TXDATA_ADDR  = SPI_BASE_ADDR + 0x48  // Tx FIFO data
  val RXDATA_ADDR  = SPI_BASE_ADDR + 0x4C  // Rx FIFO data
  val TXMARK_ADDR  = SPI_BASE_ADDR + 0x50  // Tx FIFO watermark
  val RXMARK_ADDR  = SPI_BASE_ADDR + 0x54  // Rx FIFO watermark
  val FCTRL_ADDR   = SPI_BASE_ADDR + 0x60  // spi flash interfact control
  val FFMT_ADDR    = SPI_BASE_ADDR + 0x64  // spi flash instruction format
  val IE_ADDR      = SPI_BASE_ADDR + 0x70  // spi interrupt enable
  val IP_ADDR      = SPI_BASE_ADDR + 0x74  // spi interrupt pending
}

object PlicAddr {
  def PLIC_BASE_ADDR = 0x0C000000

  def irq1_priority    = PLIC_BASE_ADDR + 0x000004  // Interrupt source 1 priority
  def irq2_priority    = PLIC_BASE_ADDR + 0x000008  // Interrupt source 2 priority
  def irq3_priority    = PLIC_BASE_ADDR + 0x00000C  // Interrupt source 3 priority
  def irq1023_priority = PLIC_BASE_ADDR + 0x000FFC  // Interrupt source 1023 priority

  def pending  = PLIC_BASE_ADDR + 0x001000  // Interrupt Pending bit 0-31
  def pendingX = PLIC_BASE_ADDR + 0x00107C  // Interrupt Pending bit 992-1023

  def enable  = PLIC_BASE_ADDR + 0x002000  // Enable bits for sources 0-31 on context 0
  def enableX = PLIC_BASE_ADDR + 0x00207F  // Enable bits for sources 992-1023 on context 0

  def threshold      = PLIC_BASE_ADDR + 0x200000  // Priority threshold for context 0
  def claim_complete = PLIC_BASE_ADDR + 0x200004  // Claim/complete for context 0
}



















