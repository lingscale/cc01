.text
.global _start

_start:
    j 2f

    lw 50 0x65
    sw t0, 0(s0)  # offset 0x00, txdata
    lw t0, 0(s2)
    li t1, 8
    srl t0, t0, t1
    sw t0, 0(s0)  # offset 0x00, txdata
 1: j 1b

 2: nop
    nop
    nop
    nop
    li t0, 0x64
    sw t0, 0(s0)  # offset 0x00, txdata
    nop
    nop


 1: j   1b
