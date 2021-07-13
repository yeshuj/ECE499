// See README.md for license details.

package systolic

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

/**
 * This is a trivial example of how to run this Specification
 * From within sbt use:
 * {{{
 * testOnly gcd.GcdDecoupledTester
 * }}}
 * From a terminal shell use:
 * {{{
 * sbt 'testOnly gcd.GcdDecoupledTester'
 * }}}
 */
class macPeekPokeTester(c: mac) extends PeekPokeTester(c)  {
  var init_counter = 0
  var curr_output = 0
  var prev_output = 0
  poke(c.io.init, 1)
  step(1)
  for {
    i <- 1 until 10
    j <- 1 until 10
  } {
    poke(c.io.init, (init_counter % 3 == 0))
    poke(c.io.init, 0)
    poke(c.io.a, i)
    poke(c.io.b, j)
    step(1)
    if(peek(c.io.out_Valid) == BigInt(1)) {
      expect(c.io.out_Data, curr_output)
    }
    curr_output = if(init_counter % 3 == 0) i*j else curr_output + i*j
    init_counter = init_counter + 1
  }

}

class macSpec extends ChiselFlatSpec {
  behavior of "macSpec"

  it should "mac computation excellently" in {
    chisel3.iotesters.Driver.execute(Array("--fint-write-vcd", "--backend-name", "verilator"), () => new mac()) { c =>
      new macPeekPokeTester(c)
    } should be(true)
  }
}

