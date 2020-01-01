module pwr_btn_rst( rst_n, rst_btn_n, clk_50m );
   output rst_n;
   input rst_btn_n;
   input clk_50m;

   reg [ 24 : 0 ] counter; /* 23bits is enough to hold 50_00000, but to reduce the chance of worst case, use 25bits */

   always @( posedge clk_50m or negedge rst_btn_n ) begin
      if ( !rst_btn_n ) begin
         counter <= 0;
      end else begin
         if ( counter != 25'd50_00000 ) /* 100ms */
            counter <= counter + 1'b1;
         else
            counter <= counter;
      end
   end

   reg rst_n_r0;
   reg rst_n_r1;

   always @( posedge clk_50m or negedge rst_btn_n ) begin
      if ( !rst_btn_n ) begin
         rst_n_r0 <= 0;
         rst_n_r1 <= 0;
      end else begin
         if ( counter == 25'd50_00000 ) begin /* worst case, counter just equal 25'd50_00000 after power on, and even worse, rst_n_r0 and rst_n_r1 equal 1, there's no power on reset */
            rst_n_r0 <= 1;
            rst_n_r1 <= rst_n_r0;
         end else begin
            rst_n_r0 <= 0;
            rst_n_r1 <= 0;
         end
      end
   end

   assign rst_n = rst_n_r1;

endmodule
