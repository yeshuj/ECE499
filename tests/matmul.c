// See LICENSE for license details.

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include "include/accumulator.h"
#include "include/translator.h"
//#include "dataset.h"


int main() {
  int *input1_data = [[1,2],[3,4]];
  int *input2_data = [[1,2],[3,4]];
  int *exp_output = [[7,10],[15,22]];

  int *output = [[0,0],[0,0]];

  doSetConfigX(1, 2);
  doSetConfigY(1, 2);
  doLoad(1, input1_data);

  doSetConfigX(2, 2);
  doSetConfigY(2, 2);
  doLoad(2, input2_data);

  doMult(1, 1,2);

  doStore(1, output);

  for(int i =0; i <2; i++){
    assert(output[i] == exp_output[i]);
  }

  return 0;
}