package chipyard

import freechips.rocketchip.config.{Config}

class Tutorial499Config extends Config(
  new ECE499.WithAccel(dim = 50, numbanks = 11) ++

  // For this demonstration we assume the base system is a single-core Rocket, for fast elaboration
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new chipyard.config.AbstractConfig
)
