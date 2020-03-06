uart_term.bin : crt0.S uart_term.c
	#crt c-run-time
	riscv32-unknown-elf-gcc -march=rv32i -g -c -o crt0.o crt0.S
	riscv32-unknown-elf-gcc -march=rv32i -g -c -o uart_term.o uart_term.c
	#riscv32-unknown-elf-ld -Ttext 0x80000000 -g crt0.o uart_term.o -o uart_term.elf
	riscv32-unknown-elf-ld -Tlink.ld -g crt0.o uart_term.o -o uart_term.elf
	riscv32-unknown-elf-objcopy -O binary -S uart_term.elf uart_term.bin
	riscv32-unknown-elf-objdump -D -m riscv -b binary uart_term.bin > test.txt
	#riscv32-unknown-elf-objdump -D -m riscv uart_term.elf > uart_term.dump
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases uart_term.elf > uart_term.dump
	#riscv32-unknown-elf-objdump -D -m riscv uart_term.elf > uart_term.dump
	#riscv32-unknown-elf-objdump -D -m riscv -M no-aliases,numeric uart_term.elf > uart_term.dump
	tail -n +8 test.txt > mem_t1.txt
	cut -c7-14 mem_t1.txt > mem_t2.txt



clean :
	rm -rf uart_term.elf uart_term.bin uart_term.dump test.txt mem*.txt *.o
