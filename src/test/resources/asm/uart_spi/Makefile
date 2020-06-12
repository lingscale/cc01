uart_spi.bin : uart_spi.S
	riscv32-unknown-elf-as -march=rv32i -o uart_spi.o uart_spi.S
	riscv32-unknown-elf-ld -Ttext 0x80000000 uart_spi.o -o uart_spi.elf
	riscv32-unknown-elf-objcopy -O binary -S uart_spi.elf uart_spi.bin
	riscv32-unknown-elf-objdump -D -m riscv -b binary uart_spi.bin > test.txt
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases uart_spi.elf > uart_spi.dump
	tail -n +8 test.txt > mem_t1.txt
	cut -c7-14 mem_t1.txt > uart_spi.txt
clean :
	rm -rf uart_spi.elf uart_spi.bin uart_spi.dump *.txt *.o
