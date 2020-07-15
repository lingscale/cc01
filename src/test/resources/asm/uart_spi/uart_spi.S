.text
.global _start
.option norvc

.equ UART_BASE_ADDR, 0x10013000
.equ SPI_BASE_ADDR, 0x10014000
.equ ITCM_ADDR, 0x80000000
_start:
   j 2f  

    csrr t0, mcause
    srli t1, t0, 24
   # li t0, 0x45
    sw t1, 0(s0)  # offset 0x00, txdata
    li t0, 0x45
    sw t0, 0(s0)  # offset 0x00, txdata
1: j 1b





2:  li s0, UART_BASE_ADDR
    li s1, SPI_BASE_ADDR
    li s2, ITCM_ADDR

    li t2, 0x80000000

    li t0, 195  # 57600 for 11.2896M
    sw t0, 24(s0)  # offset 0x18, div
    li t0, 1
    sw t0, 8(s0)  # offset 0x08, txctrl



3:  li t0, 0x68
    sw t0, 0(s0)  # offset 0x00, txdata


1:  lw t1, 0(s0)
    beq t1, t2, 1b

    li t0, 0x69
    sw t0, 0(s0)  # offset 0x00, txdata


1:  lw t1, 0(s0)
    beq t1, t2, 1b



    j 3b
