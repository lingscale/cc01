.text
.global _start
.option norvc
.equ SOURCE_ADDR, 0x20000000 + 0x30
.equ ITCM_ADDR, 0x80000000

_start:
    li s0, SOURCE_ADDR
    li s1, ITCM_ADDR
    li t1, 55
 1: lw t2, 0(s0)
    sw t2, 0(s1)
    addi s0, s0, 4
    addi s1, s1, 4
    addi t1, t1, -1
    bne t1, zero, 1b
    li ra, ITCM_ADDR
    jalr zero, 0(ra)
