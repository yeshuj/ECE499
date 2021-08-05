#ifndef MATMUL_ISA_H
#define MATMUL_ISA_H

//#include "../rocc-software/src/xcustom.h"
#define k_SET_CONFIG 0
#define k_DO_LOAD 1
#define k_DO_MULT 2
#define k_DO_STORE 3
#define k_DO_MOVE 4

#define XCUSTOM_ACC 2


// From the file rocc.h in tests directory of chipyard code **********************************************************
#include <stdint.h>

#define STR1(x) #x
#define STR(x) STR1(x)
#define EXTRACT(a, size, offset) (((~(~0 << size) << offset) & a) >> offset)

#define CUSTOMX_OPCODE(x) CUSTOM_ ## x
#define CUSTOM_0 0b0001011
#define CUSTOM_1 0b0101011
#define CUSTOM_2 0b1011011
#define CUSTOM_3 0b1111011

#define CUSTOMX(X, xd, xs1, xs2, rd, rs1, rs2, funct) \
  CUSTOMX_OPCODE(X)                     |             \
  (rd                 << (7))           |             \
  (xs2                << (7+5))         |             \
  (xs1                << (7+5+1))       |             \
  (xd                 << (7+5+2))       |             \
  (rs1                << (7+5+3))       |             \
  (rs2                << (7+5+3+5))     |             \
  (EXTRACT(funct, 7, 0) << (7+5+3+5+5))

#define ROCC_INSTRUCTION_I_R_R(X, rd, rs1, rs2, funct, rs1_n, rs2_n) {    \
    register uint64_t rs1_ asm ("x" # rs1_n) = (uint64_t) rs1;            \
    register uint64_t rs2_ asm ("x" # rs2_n) = (uint64_t) rs2;            \
    asm volatile (                                                        \
        ".word " STR(CUSTOMX(X, 0, 1, 1, rd, rs1_n, rs2_n, funct)) "\n\t" \
        :: [_rs1] "r" (rs1_), [_rs2] "r" (rs2_));                         \
  }


#define ROCC_INSTRUCTION_I_R_I(X, rd, rs1, rs2, funct, rs1_n) {         \
    register uint64_t rs1_ asm ("x" # rs1_n) = (uint64_t) rs1;          \
    asm volatile (                                                      \
        ".word " STR(CUSTOMX(X, 0, 1, 0, rd, rs1_n, rs2, funct)) "\n\t" \
        :: [_rs1] "r" (rs1_));                                          \
  }

#define ROCC_INSTRUCTION_I_I_I(X, rd, rs1, rs2, funct) {                 \
    asm volatile (                                                       \
        ".word " STR(CUSTOMX(X, 0, 0, 0, rd, rs1, rs2, funct)) "\n\t" ); \
  }

// ********************************************************************************************************************


// custom instructions
#define doSetConfig(rd, rs1, rs2)                                       \
  ROCC_INSTRUCTION_I_R_R(XCUSTOM_ACC, rd, rs1, rs2, k_SET_CONFIG, 11, 12);

#define doMult(rs1, rs2)                                         \
  ROCC_INSTRUCTION_I_I_I(XCUSTOM_ACC, 0, rs1, rs2, k_DO_MULT);

#define doMove(rd)                                         \
ROCC_INSTRUCTION_I_I_I(XCUSTOM_ACC, rd, 0, 0, k_DO_MOVE);

#define doLoad(rs1, rs2)                                    \
  asm volatile ("fence");                                 \
  ROCC_INSTRUCTION_I_R_I(XCUSTOM_ACC, 0, rs1, rs2, k_DO_LOAD, 11); \
  asm volatile ("fence");

#define doStore(rs1, rs2) \
  asm volatile ("fence");                                 \
  ROCC_INSTRUCTION_I_R_I(XCUSTOM_ACC, 0, rs1, rs2, k_DO_STORE, 11); \
  asm volatile ("fence");

#endif