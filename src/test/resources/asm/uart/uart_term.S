.text
.global _start
.option norvc
.equ UART_BASE_ADDR, 0x10013000
.equ TXDATA_ADDR,    UART_BASE_ADDR + 0x00
.equ DIV_ADDR,       UART_BASE_ADDR + 0x18

_start:
    li t1, UART_BASE_ADDR
    # 195, 57600 for 11.2896M
    # 425, 57600 for 24.576M
    # 587, 57600 for 33.8688M
    li t2, 195
    li t0, 1
    sw t2, 24(t1)
    sw t0, 8(t1)  # offset 0x08, txctrl


    li t1, TXDATA_ADDR
    li t2, 99
    li t3, 100
    li t4, 101
    li t5, 102
    li t6, 103
    sw t2, 0(t1)
# nop
    sw t3, 0(t1)
#    sw t4, 0(t1)
#    sw t5, 0(t1)
#    nop
#    nop
#    nop


 1: j   1b
