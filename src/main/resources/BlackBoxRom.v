module BlackBoxRom(
   output reg [ 31 : 0 ] q,
   input [ 31 : 0 ] addr,
   input clk
);
   reg [ 31 : 0 ] ram[ 0 : 127 ];
   initial
      $readmemh("src/main/resources/mem.txt", ram);

   always @( posedge clk )
      q <= ram[ addr[ 8 : 2 ] ];
endmodule
