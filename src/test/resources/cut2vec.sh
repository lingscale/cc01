#!/usr/local/bin/bash

tail -n +8 test.txt > mem_t1.txt
cut -c7-14 mem_t1.txt > mem.txt

cut -c7-8 mem.txt > mem_0.txt
cut -c5-6 mem.txt > mem_1.txt
cut -c3-4 mem.txt > mem_2.txt
cut -c1-2 mem.txt > mem_3.txt

rm mem_t1.txt
#rm mem.txt
