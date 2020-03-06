.text
.global _start
.global main
.option norvc
_start:
    li  sp, 0x90000100
    jal ra, main
 1: j   1b
