#define UART_BASE_ADDR   ( *( volatile unsigned int * )0x10013000 )
#define UART_TXDATA_ADDR ( *( volatile unsigned int * )0x10013000 )
#define UART_DIV_ADDR    ( *( volatile unsigned int * )0x10013018 )

int main() {

   //int a[] = { 99, 99, 48, 49, 32, 104, 32, 98, 32, 108, 105, 110, 103, 115, 99, 97, 108, 101, 32,  105, 99, 114, 111, 115, 121, 115, 116, 101, 109 }; 
   int a[] = { 99, 99, 48, 49 };

   int i;
   int rdata;


   UART_DIV_ADDR = 195; // 57600 for 11.2896M
   //UART_DIV_ADDR = 425; // 57600 for 24.576M
   //UART_DIV_ADDR = 587; // 57600 for 33.8688M
   //UART_DIV_ADDR = 867;  // 57600 for 50M

   for ( i = 0; i < 4; i++ ) {
      UART_TXDATA_ADDR = a[ i ];
      rdata = UART_TXDATA_ADDR;
      while ( rdata == 0x80000000 ) {
         rdata = UART_TXDATA_ADDR;
      };
   }

   return 0;
}
