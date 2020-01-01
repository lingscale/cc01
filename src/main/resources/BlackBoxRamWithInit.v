module BlackBoxRamWithInit(
   output reg [ 31 : 0 ] q,
   input [ 31 : 0 ] d,
   input [ 9 : 0 ] write_address, read_address,
   input wen,
   input clk
);

   reg [ 31 : 0 ] mem[ 0 : 1023 ];
   initial
   begin
      $readmemh("src/test/resources/mem.txt", mem);
   end

   always @( posedge clk ) begin
      if ( wen )
         mem[ write_address ] <= d;
      q <= mem[ read_address ];
   end
endmodule
