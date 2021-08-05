# MATRIX MULTIPOLICATION ACCELERATOR

## Simulation
In order to simulate the accelerator, following steps must be followed:
1. Install RISCV tools following steps in the https://github.com/chipsalliance/rocket-tools/ repository. This will install all the tools needed for running the simulation including the gnu toolchain.
2. Once the tools have been installed, clone the https://github.com/ucb-bar/chipyard repository. This repository is used to produce a RISC V system on chip with rocket or BOOM cores along with MMIO mapped peripherals and coprocessors. For this project, a single rocket core with the matrix multiplication accelerator will be generated.
3. Add the project repository as a submodule to the chipyard repository in the generators folder using the following command.
```
cd generators
git submodule add https://github.com/yeshuj/ECE499.git
```

4. Copy the test cases from software/ directory in the yeshuj/ECE499 repository and paste it in tests/ directory in the chipyard repository.
5. In the tests/ directory, run the `make` command. This will install libgloss locally if it's not installed, and will compile the test cases to a .riscv binary file which can be run on the simulator.
6. Add a config similar to the one shown in figure 7 to the file “generators/chipyard/src/main/scala/config/TutorialConfigs.scala”. This config generates a simple SoC with just a rocket core and the accelerator with designated opcode 2.
7. Run the following command in the sims/verilator directory to run the simulation.
```
make -j8 CONFIG=Tutorial499Config run-binary-debug BINARY=../../tests/matmul.riscv
```

