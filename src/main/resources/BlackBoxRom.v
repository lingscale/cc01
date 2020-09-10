module BlackBoxRom(
   output [ 31 : 0 ] q,
   input [ 31 : 0 ] addr,
   input read,
   input clk
);

   reg [ 31 : 0 ] ram[ 0 : 63 ];
   reg [ 5 : 0 ] read_addr;

   initial
      $readmemh("src/main/resources/mem.txt", ram);

   always @( posedge clk ) begin
      if (read) read_addr <= addr[ 7 : 2 ];
   end

   assign q = ram[ read_addr ];

endmodule
