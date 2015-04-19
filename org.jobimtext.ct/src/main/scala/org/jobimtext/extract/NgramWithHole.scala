/*
 *
 *  Copyright 2015.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.jobimtext.extract

import java.util

import org.apache.spark.rdd.RDD
import scala.math.{min,max}
import scala.reflect.internal.util.Collections


/**
 * Created by Steffen Remus.
 */
object NgramWithHole {

  def apply(n:Int = 3, allcombinations:Boolean = false, lines_in:RDD[String]):RDD[String] = {
    if(allcombinations)
      return lines_in.flatMap(getAllCombinations(_,n))
    else
      return lines_in.flatMap(getCenterCombinations(_,n))
  }

  def getCenterCombinations(line:String, n:Int=3):TraversableOnce[String] = {

    val id = Integer.toHexString(line.hashCode)
    val tokens = line.split(' ')
    if(tokens.size < n)
      return Traversable.empty // FIXME: too short sequences are currently ignored
    val ngrams = tokens.sliding(n).map(_.toSeq)
    val m = n/2

    val result_ngram_limits_begin = for (i <- 0 until min(m, tokens.length))
      yield "%s\t%s\t%s".format(tokens(i), "%s @ %s".format(tokens.slice(0, i).mkString(" "), tokens.slice(i+1,n).mkString(" ")).trim, id)

    val result_ngram_limits_end = for (i <- max(tokens.length-m,0) until tokens.length)
      yield "%s\t%s\t%s".format(tokens(i), "%s @ %s".format(tokens.slice(tokens.length-n, i).mkString(" "), tokens.slice(i+1, tokens.length).mkString(" ")).trim, id)

    val result_ngram_center = ngrams.map(ngram => "%s\t%s\t%s".format(ngram(m), "%s @ %s".format(ngram.take(m).mkString(" "), ngram.takeRight(n-1-m).mkString(" ")).trim(), id))

    return result_ngram_limits_begin++result_ngram_center++result_ngram_limits_end

  }

  def getAllCombinations(line:String, n:Int=3):TraversableOnce[String] = {
    val id = Integer.toHexString(line.hashCode)
    val tokens = line.split(' ')
    if(tokens.size < n)
      return Traversable.empty // FIXME: too short sequences are currently ignored
    val ngrams = tokens.sliding(n).map(_.toSeq)
    ngrams.flatMap(ngram =>
      for (i <- 0 until ngram.length)
        yield "%s\t%s\t%s".format(ngram(i), "%s @ %s".format(ngram.take(i).mkString(" "), ngram.takeRight(n-1-i).mkString(" ")).trim(), id)
    )
  }


  def main(args: Array[String]) {
    NgramWithHole.getAllCombinations("a b c",3).foreach(println(_))
    println("---")
    NgramWithHole.getCenterCombinations("a b c",3).foreach(println(_))
  }
}