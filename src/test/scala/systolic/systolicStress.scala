// See README.md for license details.

package systolic

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import org.scalacheck.Prop.True

import scala.util._
import scala.util.control.Breaks.break

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
class systolicStressTester(c: systolic_d, size: Int) extends PeekPokeTester(c)  {
  var init_counter = 0
  var curr_output = 0
  var prev_output = 0

  val matrix = (for (i <- 0 until size) yield (for(j <- 0 until size) yield Random.nextInt(100)).toArray).toArray
  val matrix2 = (for (i <- 0 until size) yield (for(j <- 0 until size) yield Random.nextInt(100)).toArray).toArray
  val matrixMul = (for (i <- 0 until size) yield (for(j <- 0 until size) yield (for (k <- 0 until size) yield matrix(i)(k) * matrix2(k)(j)).sum).toArray).toArray
  print(matrixMul(0)(1))
  poke(c.io.calc, 0)
  step(1)
  for {
    i <- 0 until size
  } {
    poke(c.io.calc, 1)
    for(j <- 0 until size) {
      poke(c.io.in_A.data(j), matrix(j)(i))
      poke(c.io.in_B.data(j), matrix2(i)(j))
    }
    step(1)
  }
  poke(c.io.calc, 0)
  step(size)
  expect(c.io.out.fire(), true)
  var num_cal = size
  while(num_cal != -1*size+1){
    for(i <- 0 until size){
      expect(c.io.out.fire(), true)
      if(num_cal+i-1 < size && num_cal+i-1 >= 0) {
        expect(c.io.out.bits.data(i), matrixMul(i)(num_cal + i - 1))
      }
    }
    num_cal -= 1
    step(1)
  }
}

class systolicStressSpec extends ChiselFlatSpec {
  behavior of "systolicSpec"

  it should "array excellently" in {
    val args = Array("-o", "mnist_cnn_v4.v",
      "-X", "verilog",
      "--no-check-comb-loops",
      "--no-dce",
      "--info-mode=ignore")

    chisel3.iotesters.Driver.execute(args, () => new systolic_d(100, 64)) { c =>
      new systolicStressTester(c, 100)
    } should be(true)
  }
}

