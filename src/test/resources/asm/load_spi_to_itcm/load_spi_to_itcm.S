.text
.global _start
.option norvc

.equ UART_BASE_ADDR, 0x10013000
.equ SPI_BASE_ADDR, 0x10014000
.equ ITCM_ADDR, 0x80000000

_start:
    li s0, UART_BASE_ADDR
    li s1, SPI_BASE_ADDR
    li s2, ITCM_ADDR

    li t0, 97  # 195, 57600 for 11.2896M; 97, 115200 for 11.2896M
    sw t0, 24(s0)  # offset 0x18, div
    li t0, 1
    sw t0, 8(s0)  # offset 0x08, txctrl
    li t0, 0x63
    sw t0, 0(s0)  # offset 0x00, txdata
    li t0, 0x63
    sw t0, 0(s0)
    li t0, 0x30
    sw t0, 0(s0)
    li t0, 0x31
    sw t0, 0(s0)
    li t0, 0x0d
    sw t0, 0(s0)
    li t0, 0x0a
    sw t0, 0(s0)
    sw t0, 0(s0)

    li t1, 3       # Read Data Instruction (03h)
    sw t1, 72(s1)  # txdata, offset 0x48
    li t1, 16      # 24-Bit Address, 1M
    sw t1, 72(s1)
    li t1, 0
    sw t1, 72(s1)
    li t1, 0
    sw t1, 72(s1)
    li t1, 0xfffffffe # just enable simple spi SS pin
    sw t1, 20(s1)  # csdef, offset 0x14

#    li t2, 512  # 128 * 4 bytes
#    li t2, 4096  # 1k * 4 bytes
    li t2, 65536  # 16k * 4 bytes
    li t3, 0x80000000  # rxfifio empty
 1: lw t1, 76(s1)  # rxdata, offset 0x4C
    bgeu t1, t3, 1b
    sb t1, 0(s2)
    addi s2, s2, 1
    addi t2, t2, -1
    bne t2, zero, 1b
    li t1, 0xffffffff  # disable simple spi SS pin
    sw t1, 20(s1)  # csdef, offset 0x14

 1: lw t1, 76(s1)  # clear spi receive fifo
    bge t1, zero, 1b

    li ra, ITCM_ADDR
    jalr zero, 0(ra)









