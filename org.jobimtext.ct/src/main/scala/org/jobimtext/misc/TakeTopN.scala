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

package org.jobimtext.misc

import org.apache.spark.rdd.RDD
import org.jobimtext.util.FixedSizeTreeSet

/**
 * Created by Steffen Remus.
 */
object TakeTopN {

  val ord = new Ordering[(String, Double)] {
    def compare(o1:(String, Double), o2:(String, Double)): Int = {
      val r = o1._2.compareTo(o2._2)
      if(r != 0)
        return r
      return o1._1.compareTo(o2._1)
    }
  }

  val ord_rev = new Ordering[(String, Double)] {
    def compare(o1:(String, Double), o2:(String, Double)): Int = {
      val r = o2._2.compareTo(o1._2)
      if(r != 0)
        return r
      return o1._1.compareTo(o2._1)
    }
  }

  /**
   *
   * @param lines_in (e1,e2,value)
   * @param descending ordering of top n
   * @return (e1,e1,value) pruned to top n
   */
  def apply(n:Int, descending:Boolean, sortbykey:Boolean = false, lines_in:RDD[String]):RDD[String] = {

    var inner_sorted = lines_in.map(_.split("\t"))
      .map({case Array(e1,e2,value) => (e1, (e2, value.toDouble), FixedSizeTreeSet.empty(if (descending) ord_rev else ord, n))})
      .map({case (o1,tupl,sortedset) => (o1, (sortedset+=(tupl)))})
      .reduceByKey((r,c) => (r++=(c)))

    if(sortbykey)
      inner_sorted = inner_sorted.sortByKey()

    val lines_out = inner_sorted
      .flatMap({case(o1, s) => s.toSeq.map({case (o2,sim) => (o1,o2,sim)})})
      .map({case (o1,o2,sim) => "%s\t%s\t%e".format(o1,o2,sim)})

    return lines_out

  }

}