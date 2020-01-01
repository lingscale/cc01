module BlackBoxSinglePortRom(
   output reg [ 31 : 0 ] q,
   input [ 6 : 0 ] addr,
   input clk
);
   reg [ 31 : 0 ] ram[ 0 : 127 ];
   initial
      $readmemh("src/main/resources/mem.txt", ram);

   always @( posedge clk )
      q <= ram[ addr ];
endmodule
