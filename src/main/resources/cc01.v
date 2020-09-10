module cc01(
   output io_tx,
   input io_rx,
   input rst_btn_n,
   input clk,

   input   io_clockRTC,
   output  io_MOSI,
   input   io_MISO,
   output  io_SS,
   output  io_SCK,
   output [7:0]  io_led8,
   output [31:0] io_led32

);
   
   wire rst_n;

   wire xled8;

   Soc soc( .clock( clk ), .reset( ~rst_n ), .io_tx( io_tx ), .io_rx( io_rx ), .io_clockRTC( io_clockRTC ),
            .io_MOSI( io_MOSI ), .io_MISO( io_MISO ), .io_SS( io_SS ), .io_SCK( io_SCK ), .io_led8( xled8 ), .io_led32( io_led32 ) );

   assign io_led8[5:0] = xled8[5:0];
   assign io_led8[7] = rst_n;
   assign io_led8[6] = rst_btn_n;

   pwr_btn_rst p_b_rst( .rst_n( rst_n ), .rst_btn_n( rst_btn_n ), .clk_50m( clk ) );

endmodule
