#ifndef MATMUL_ISA_H
#define MATMUL_ISA_H

#include "rocc-software/src/xcustom.h"

#define k_SET_CONFIG_X 0
#define k_SET_CONFIG_Y 1
#define k_DO_LOAD 2
#define k_DO_MULT 3
#define k_DO_STORE 4

#define XCUSTOM_ACC 1

#define doSetConfigX(rs1, rs2)                                       \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_ACC, rs1, rs2, k_SET_CONFIG_X);
#define doSetConfigY(rs1, rs2)                                       \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_ACC, rs1, rs2, k_SET_CONFIG_Y);
#define doLoad(rd, rs1, rs2)                                         \
  ROCC_INSTRUCTION_R_R_R(XCUSTOM_ACC, rd, rs1, rs2, k_DO_LOAD);
#define doMult(rs1, rs2)                                    \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_ACC, rs1, rs2, k_DO_MULT);
#define doStore(rs1, rs2) \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_ACC, rs1, rs2, k_DO_STORE);

#endif