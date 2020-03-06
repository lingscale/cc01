uart_term.bin : uart_term.S
	riscv32-unknown-elf-as -march=rv32i -o uart_term.o uart_term.S
	riscv32-unknown-elf-ld -Ttext 0x20000000 uart_term.o -o uart_term.elf
	riscv32-unknown-elf-objcopy -O binary -S uart_term.elf uart_term.bin
	riscv32-unknown-elf-objdump -D -m riscv -b binary uart_term.bin > test.txt
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases uart_term.elf > uart_term.dump
	tail -n +8 test.txt > mem_t1.txt
	cut -c7-14 mem_t1.txt > mem_t2.txt
clean :
	rm -rf uart_term.elf uart_term.bin uart_term.dump *.txt *.o
