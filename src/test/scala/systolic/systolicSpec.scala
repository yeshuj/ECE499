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
class systolicPeekPokeTester(c: systolic) extends PeekPokeTester(c)  {
  var init_counter = 0
  var curr_output = 0
  var prev_output = 0

  poke(c.io.rst, 1)
  step(1)
//  for {
//    i <- 1 until 4
//  } {
  poke(c.io.init, 0)
  poke(c.io.rst, 1)
  step(1)
  poke(c.io.rst, 0)
  poke(c.io.A(0), 1)
  poke(c.io.A(1), 2)
  poke(c.io.B(0), 1)
  poke(c.io.B(1), 2)
  step(1)
  poke(c.io.A(0), 3)
  poke(c.io.A(1), 4)
  poke(c.io.B(0), 3)
  poke(c.io.B(1), 4)
  step(1)
  poke(c.io.init, 1)
  step(1)
  poke(c.io.init, 0)
  poke(c.io.A(0), 0)
  poke(c.io.A(1), 0)
  poke(c.io.B(0), 0)
  poke(c.io.B(1), 0)
  step(2)
  expect(c.io.Valid(0), true)
  step(3)
}

class systolicSpec extends ChiselFlatSpec {
  behavior of "systolicSpec"

  it should "array excellently" in {
    chisel3.iotesters.Driver.execute(Array("--fint-write-vcd", "--backend-name", "verilator"), () => new systolic(2)) { c =>
      new systolicPeekPokeTester(c)
    } should be(true)
  }
}

