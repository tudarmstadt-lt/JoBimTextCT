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

package org.jobimtext.run

import de.tudarmstadt.lt.scalautils.FixedSizeTreeSet
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.jobimtext.ct2
import org.jobimtext.ct2.ClassicToCT
import org.jobimtext.ct2.sig.ProbsFromCT
import org.jobimtext.misc.{JoinBySharedFeaturesCartesian, TakeTopN}
import org.jobimtext.sim._
import org.jobimtext.spark.SparkConfigured

import scala.tools
import scala.tools.reflect
import scala.reflect.runtime._

/**
 * Created by Steffen Remus.
 */
object SparkRunner extends SparkConfigured{

  def main(args: Array[String]):Unit = {
    run(args)
//    val tb = tools.reflect.ToolBox(universe.runtimeMirror(getClass.getClassLoader)).mkToolBox()
//    println(tb.parse("println(\"hello world\")"))

  }

  override def run(conf:SparkConf, args: Array[String]): Unit = {

    val cp = conf.getOption("checkpoint").getOrElse({println("Create checkpoints: '%s'.".format(true)); true}).asInstanceOf[Boolean]
    val topnfeatures = conf.getOption("topnf").getOrElse({println("Setting 'topnf' to '%d'.".format(1000)); 1000}).asInstanceOf[Int]
    val sort_out = conf.getOption("sort").getOrElse({println("Sort output: '%s'.".format(false)); false}).asInstanceOf[Boolean]

    val in = conf.getOption("in").getOrElse(throw new IllegalStateException("Missing input path. Specify with '-in=<file-or-dir>'."))
    val out = conf.getOption("out").getOrElse(throw new IllegalStateException("Missing output path. Specify with '-out=<dir>'."))

    conf.setAppName("JoBimTextCT")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array(classOf[FixedSizeTreeSet[_]], classOf[ct2.CT2[_]]))

    val sc = new SparkContext(conf)

    val kl = run(sc,in,out,cp,topnfeatures,sort_out)

    sc.stop()

  }

  def run(sc:SparkContext,
          in:String,
          out:String,
          checkpoint:Boolean,
          topnfeatures:Int = 1000,
          sort_output:Boolean = false,
          reverse_sorting:Boolean = false,
          trimtopn:Int = 20
           ):RDD[String] = {

    val lines_in = sc.textFile(in).filter(_.nonEmpty)
    val cts = ClassicToCT.classicWordFeatureCountToAggregatedCT2(lines_in)
    if(checkpoint)
      cts.saveAsTextFile(out + "_ct")

    val probs = TakeTopN(topnfeatures, true, false, ProbsFromCT(cts))
    if(checkpoint)
      probs.saveAsTextFile(out + "_p")

    val joinedprobs = JoinBySharedFeaturesCartesian(probs)
    if(checkpoint)
      joinedprobs.saveAsTextFile(out + "_jp")

    var kl = KLDivergence(joinedprobs)
    if(sort_output)
      kl = TakeTopN(n = trimtopn, descending = reverse_sorting, true, kl)

    kl.saveAsTextFile(out + "_kl")

    //    var cts:RDD[String] = null
    //    try {
    //      cts = sc.textFile(out + "_ct").filter(_.nonEmpty) // unfortunately the error is not thrown here but when cts is used
    //    } catch {
    //      case e => {
    //        e.printStackTrace()
    //        val lines_in = sc.textFile(in).filter(_.nonEmpty)
    //        cts = ClassicToCT.classicWordFeatureCountToAggregatedCT2(lines_in)
    //        cts.saveAsTextFile(out + "_ct");
    //      }
    //    }

    return kl
  }



}
