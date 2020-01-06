#!/usr/local/bin/bash



for((i=0;i<2050;i++)); do
echo "00000013" >> mem.txt
done

head -n 2050 mem.txt > mem2.txt


# paste -d "," mem_3.txt mem_2.txt mem_1.txt mem_0.txt > mem.txt
