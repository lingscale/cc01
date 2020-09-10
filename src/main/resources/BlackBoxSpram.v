module BlackBoxSpram( dataOut, dataIn, addr, write, mask, enable, clk);

   output [ 31 : 0 ] dataOut;
   input [ 31 : 0 ] dataIn;
   input [ 31 : 0 ] addr;
   input write;
   input [ 3 : 0 ] mask;
   input enable;
   input clk;

//`define iCEcube2  // Lattice iCEcube2 or yosys
`define Radiant   // Lattice Radiant

`ifdef iCEcube2
   SB_SPRAM256KA spram_0(
      .DATAIN( dataIn[ 15 : 0 ] ),
      .ADDRESS( addr[ 15 : 2 ] ),
      .MASKWREN( { mask[ 1 ], mask[ 1 ], mask[ 0 ], mask[ 0 ] } ),
      .WREN( write ),
      .CHIPSELECT( enable ),
      .CLOCK( clk ),
      .STANDBY( 1'b0 ),
      .SLEEP( 1'b0 ),
      .POWEROFF( 1'b1 ),
      .DATAOUT( dataOut[ 15 : 0 ] )
   );

   SB_SPRAM256KA spram_1(
      .DATAIN( dataIn[ 31 : 16 ] ),
      .ADDRESS( addr[ 15 : 2 ] ),
      .MASKWREN( { mask[ 3 ], mask[ 3 ], mask[ 2 ], mask[ 2 ] } ),
      .WREN( write ),
      .CHIPSELECT( enable ),
      .CLOCK( clk ),
      .STANDBY( 1'b0 ),
      .SLEEP( 1'b0 ),
      .POWEROFF( 1'b1 ),
      .DATAOUT( dataOut[ 31 : 16 ] )
   );
`endif

`ifdef Radiant
   SP256K spram_0(
      .AD( addr[ 15 : 2 ] ),
      .DI( dataIn[ 15 : 0 ] ),
      .MASKWE( { mask[ 1 ], mask[ 1 ], mask[ 0 ], mask[ 0 ] } ),
      .WE( write ),
      .CS( enable ),
      .CK( clk ),
      .STDBY( 1'b0 ),
      .SLEEP( 1'b0 ),
      .PWROFF_N( 1'b1 ),
      .DO( dataOut[ 15 : 0 ] )
   );

   SP256K spram_1(
      .AD( addr[ 15 : 2 ] ),
      .DI( dataIn[ 31 : 16 ] ),
      .MASKWE( { mask[ 3 ], mask[ 3 ], mask[ 2 ], mask[ 2 ] } ),
      .WE( write ),
      .CS( enable ),
      .CK( clk ),
      .STDBY( 1'b0 ),
      .SLEEP( 1'b0 ),
      .PWROFF_N( 1'b1 ),
      .DO( dataOut[ 31 : 16 ] )
   );
`endif

endmodule
