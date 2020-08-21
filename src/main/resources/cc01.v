module cc01(
   output io_tx,
   input io_rx,
   input rst_btn_n,
   input clk,

   input   io_clockRTC,
   output  io_MOSI,
   input   io_MISO,
   output  io_SS,
   output  io_SCK

);
   
   wire rst_n;   
   Soc soc( .clock( clk ), .reset( ~rst_n ), .io_tx( io_tx ), .io_rx( io_rx ), .io_clockRTC( io_clockRTC ),
            .io_MOSI( io_MOSI ), .io_MISO( io_MISO ), .io_SS( io_SS ), .io_SCK( io_SCK ) );

   pwr_btn_rst p_b_rst( .rst_n( rst_n ), .rst_btn_n( rst_btn_n ), .clk_50m( clk ) );

endmodule