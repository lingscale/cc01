#!/usr/local/bin/bash

riscv32-unknown-elf-objcopy -O binary -S test.elf test.bin
riscv32-unknown-elf-objdump -D -m riscv -b binary test.bin > test.txt

rm test.bin

tail -n +8 test.txt > mem_t1.txt
cut -c11-18 mem_t1.txt > mem.txt


cut -c7-8 mem.txt > mem_0.txt
cut -c5-6 mem.txt > mem_1.txt
cut -c3-4 mem.txt > mem_2.txt
cut -c1-2 mem.txt > mem_3.txt


# paste -d "," mem_3.txt mem_2.txt mem_1.txt mem_0.txt > mem.txt
