#define UART_BASE_ADDR   ( *( volatile unsigned int * )0x10013000 )
#define UART_TXDATA_ADDR ( *( volatile unsigned int * )0x10013000 )
#define UART_DIV_ADDR    ( *( volatile unsigned int * )0x10013018 )

int main() {

   const char * cc01_msg = "\
Welcome to cc01!\r\n\
cc01 is a simple RISC written in chisel3.\r\n\
It only supports 32bits integer instructions.\r\n\
cc means chisel cat, or something else, or just cc.\r\n\
Never mind, just a name.\r\n\
Feel free, and first of all, appreciated, to point out bugs.\r\n\
\r\n\
";

   int i;
   int rdata;

   UART_DIV_ADDR = 195; // 57600 for 11.2896M
   //UART_DIV_ADDR = 425; // 57600 for 24.576M
   //UART_DIV_ADDR = 587; // 57600 for 33.8688M
   //UART_DIV_ADDR = 867;  // 57600 for 50M

   i = 0;
   while ( cc01_msg[ i ] != '\0' ) {
      UART_TXDATA_ADDR = cc01_msg[ i ];
      rdata = UART_TXDATA_ADDR;
      while ( rdata == 0x80000000 ) {
         rdata = UART_TXDATA_ADDR;
      };
      i++;
   }

   return 0;
}
