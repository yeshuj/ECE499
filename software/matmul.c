// See LICENSE for license details.

//#include <assert.h>
//#include <stdint.h>
//#include <stdio.h>
#include "matmul_isa.h"
//#include "util.h"
//#include "dataset.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <riscv-pk/encoding.h>


int test1(){

  long input1_data[2][2] = {{1,2},{3,4}};
  long input2_data[2][2] = {{1,2},{3,4}};
  long exp_output[2][2] = {{7,10},{15,22}};

  long output[2][2] = {{0,0},{0,0}};

  doSetConfig(0, 2, 2)
  doLoad(input1_data, 0);

  doSetConfig(1, 2, 2);
  doLoad(input2_data, 1);

  doSetConfig(2, 2, 2);
  doMult(0, 1);

  doMove(2);

  doStore(output, 2);

  for(int i =0; i < 2; i++){
    for(int j = 0; j < 2; j++){
        if(output[i][j] != exp_output[i][j]){
                    printf("%lu : %x : %ld : %ld\n", rdcycle(), &(output[i][j]), output[i][j], exp_output[i][j]);
          return -1;
        }
    }
  }
  printf("passed test 1\n");
  return 0;
}

int test2(){
  long input1_data[1][2] = {{1,3}};
  long input2_data[2][1] = {{4},{5}};
  long exp_output[1][1] = {{19}};

  long output[1][1] = {{0}};

  doSetConfig(0, 1, 2)
  doLoad(input1_data, 0);

  doSetConfig(1, 2, 1);
  doLoad(input2_data, 1);

  doSetConfig(2, 1, 1);
  doMult(0, 1);

  doMove(2)

  doStore(output, 2);

  for(int i =0; i < 1; i++){
    for(int j = 0; j < 1; j++){
        if(output[i][j] != exp_output[i][j]){
                    printf("%lu : %x : %ld : %ld\n", rdcycle(), &(output[i][j]), output[i][j], exp_output[i][j]);
            return -1;
        }
    }
  }
    printf("passed test 2\n");
  return 0;
}

int test3(){
  long input1_data_A[4][4] = {{1,2,3,4}, {5,4,3,2}, {1,2,3,4}, {5,4,3,2}};
  long input1_data_B[4][1] = {{5}, {1}, {5}, {1}};
  long input2_data_A[4][4] = {{1,2,3,4}, {4,3,2,1}, {1,2,3,4}, {4,3,2,1}};
  long input2_data_B[1][4] = {{1,1,1,1}};
  long exp_output[4][4] = {{33,31,29,27}, {33,35,37,39}, {33,31,29,27}, {33,35,37,39}};

  long output[4][4] = {{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}};

  doSetConfig(0, 4, 4)
  doLoad(input1_data_A, 0);

  doSetConfig(1, 4, 1)
  doLoad(input1_data_B, 1);

  doSetConfig(2, 4, 4);
  doLoad(input2_data_A, 2);

  doSetConfig(3, 1, 4)
  doLoad(input2_data_B, 3);

//  doSetConfig(2, 1, 1);
  doMult(0, 2);
  doMult(1, 3);

  doMove(0);

  doStore(output, 0);

  for(int i =0; i < 4; i++){
    for(int j = 0; j < 4; j++){
        if(output[i][j] != exp_output[i][j]){
            printf("%lu : %x : %ld : %ld\n", rdcycle(), &(output[i][j]), output[i][j], exp_output[i][j]);
            return -1;
        }
    }
  }
    printf("passed test 3\n");
  return 0;
}


int main(int argc, char* argv[]) {
  if(test1() == -1){
    return -1;
  }
  if(test2() == -1){
    return -1;
  }
  if(test3() == -1){
    return -1;
  }

  return 0;
}