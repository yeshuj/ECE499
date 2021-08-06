#!/usr/bin/env bash

cd chipyard/tests/ && make PROGRAMS=matmul
cd ../sims/verilator
make -j8 CONFIG=Tutorial499Config run-binary-debug BINARY=../../tests/matmul.riscv