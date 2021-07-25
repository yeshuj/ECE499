#include <stdio.h>

int main(){
//  printf("\n[[FAILED]]\n");
  int a,b,c;
  a = 5;
  b = 2;
  asm volatile
  (
    "add   %[z], %[x], %[y]\n\t"
    : [z] "=r" (c)
    : [x] "r" (a), [y] "r" (b)
  );

  if ( c != 7 ){
//     printf("\n[[FAILED]]\n");
     return -1;
  }

//  printf("\n[[PASSED]]\n");

  return 0;
}