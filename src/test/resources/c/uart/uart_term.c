#define UART_BASE_ADDR   ( *( volatile unsigned int * )0x10013000 )
#define UART_TXDATA_ADDR ( *( volatile unsigned int * )0x10013000 )
#define UART_RXDATA_ADDR ( *( volatile unsigned int * )0x10013004 )
#define UART_TXCTRL_ADDR ( *( volatile unsigned int * )0x10013008 )
#define UART_RXCTRL_ADDR ( *( volatile unsigned int * )0x1001300C )
#define UART_IE_ADDR     ( *( volatile unsigned int * )0x10013010 )
#define UART_IP_ADDR     ( *( volatile unsigned int * )0x10013014 )
#define UART_DIV_ADDR    ( *( volatile unsigned int * )0x10013018 )

//#include <string.h>

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
   UART_TXCTRL_ADDR = 1;

/*
   while ( *( cc01_msg ) != '\0' ) {
      UART_TXDATA_ADDR = *( cc01_msg );
      rdata = UART_TXDATA_ADDR;
      while ( rdata == 0x80000000 ) {
         rdata = UART_TXDATA_ADDR;
      }
      cc01_msg++;
   }
*/

   // int len = strlen( cc01_msg );
   for ( i = 0; i <= 250; i++ ) {
      UART_TXDATA_ADDR = cc01_msg[ i ];
      rdata = UART_TXDATA_ADDR;
      while ( rdata == 0x80000000 ) {
         rdata = UART_TXDATA_ADDR;
      };
   }

   return 0;
}
