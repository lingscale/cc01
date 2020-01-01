#!/usr/local/bin/bash

riscv32-as  -o test.o test.s
riscv32-ld -Ttext 0x80000000 test.o -o test.elf
riscv32-objdump -D test.elf > test.txt

riscv32-unknown-elf-as -o test.o test.s
riscv32-unknown-elf-ld  -Ttext 0x80000000 test.o -o test.elf
riscv32-unknown-elf-objcopy -O binary -S test.elf test.bin
riscv32-unknown-elf-objdump -D -m riscv -b binary test.bin > test.txt
riscv32-unknown-elf-objdump -D test.elf > test.dump

tail -n +8 test.txt > mem_t1.txt
#cut -c11-18 mem_t1.txt > mem_t2.txt
cut -c7-14 mem_t1.txt > mem_t2.txt

for((i=0;i<16384;i++)); do
echo "00000013" >> mem_t2.txt
done

head -n 16384 mem_t2.txt > mem.txt
rm mem_t*.txt

cut -c7-8 mem.txt > mem_0.txt
cut -c5-6 mem.txt > mem_1.txt
cut -c3-4 mem.txt > mem_2.txt
cut -c1-2 mem.txt > mem_3.txt

rm mem_t*
# paste -d "," mem_3.txt mem_2.txt mem_1.txt mem_0.txt > mem.txt
