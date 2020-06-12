module BlackBoxRom(
   output reg [ 31 : 0 ] q,
   input [ 31 : 0 ] addr,
   input read,
   input clk
);
   reg [ 31 : 0 ] ram[ 0 : 127 ];
   initial
      $readmemh("src/main/resources/mem.txt", ram);

   always @( posedge clk ) begin
      if ( read ) q <= ram[ addr[ 8 : 2 ] ];
   end
endmodule
