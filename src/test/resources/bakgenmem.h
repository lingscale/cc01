#!/usr/local/bin/bash

#riscv64-unknown-freebsd12.0-as -march rv32i -o test.elf test.s
#riscv64-unknown-freebsd12.0-objdump -D test.elf > mem.txt

riscv32-as  -o test.o test.s
riscv32-ld -Ttext 0x80000000 test.o -o test.elf
riscv32-objdump -D test.elf > test.txt
rm test.o
rm test.elf

tail -n +8 test.txt > mem_t1.txt
cut -c11-20 mem_t1.txt > mem_t2.txt

for((i=0;i<16384;i++)); do
echo "00000013" >> mem_t2.txt
done

head -n 16384 mem_t2.txt > mem.txt
rm mem_t*.txt

cut -c7-8 mem.txt > mem_0.txt
cut -c5-6 mem.txt > mem_1.txt
cut -c3-4 mem.txt > mem_2.txt
cut -c1-2 mem.txt > mem_3.txt

# paste -d "," mem_3.txt mem_2.txt mem_1.txt mem_0.txt > mem.txt
