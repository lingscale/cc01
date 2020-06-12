load_spi_to_itcm.bin : load_spi_to_itcm.S
	riscv32-unknown-elf-as -march=rv32i -o load_spi_to_itcm.o load_spi_to_itcm.S
	riscv32-unknown-elf-ld -Ttext 0x20000000 load_spi_to_itcm.o -o load_spi_to_itcm.elf
	riscv32-unknown-elf-objcopy -O binary -S load_spi_to_itcm.elf load_spi_to_itcm.bin
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases -b binary load_spi_to_itcm.bin > test.txt
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases load_spi_to_itcm.elf > load_spi_to_itcm.dump
	tail -n +8 test.txt > mem_t1.txt
	cut -c7-14 mem_t1.txt > load_spi_to_itcm.txt
clean :
	rm -rf load_spi_to_itcm.elf load_spi_to_itcm.bin load_spi_to_itcm.dump *.txt *.o
