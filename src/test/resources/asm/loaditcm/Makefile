loaditcm.bin : loaditcm.S
	riscv32-unknown-elf-as -march=rv32i -o loaditcm.o loaditcm.S
	riscv32-unknown-elf-ld -Ttext 0x20000000 loaditcm.o -o loaditcm.elf
	riscv32-unknown-elf-objcopy -O binary -S loaditcm.elf loaditcm.bin
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases -b binary loaditcm.bin > test.txt
	riscv32-unknown-elf-objdump -D -m riscv -M no-aliases loaditcm.elf > loaditcm.dump
clean :
	rm -rf loaditcm.elf loaditcm.bin loaditcm.dump test.txt *.o
