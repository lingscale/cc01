.text
.global _start
.option norvc
.equ UART_BASE_ADDR, 0x10013000
.equ TXDATA_ADDR,    UART_BASE_ADDR + 0x00
.equ DIV_ADDR,       UART_BASE_ADDR + 0x18

_start:
    li t1, DIV_ADDR
    li t2, 425
    sw t2, 0(t1)
    li t1, TXDATA_ADDR
    li t2, 0b10110111
    sw t2, 0(t1)
 1: j   1b
