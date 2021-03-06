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

package org.jobimtext

import de.tudarmstadt.lt.scalautils.FixedSizeTreeSet
import org.apache.hadoop.mapred.InvalidInputException
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import org.jobimtext._
import org.jobimtext.ct2.{CT2, AggregateCT, ClassicToCT}
import org.jobimtext.extract.{NgramWithHole, CooccurrenceWindow}
import org.jobimtext.misc._
import org.jobimtext.sim._

/**
 * Created by Steffen Remus.
 */
object TestRunnerSpark {

  def main(args: Array[String]) {
    testSampleSentences
//    testArtificalData
  }


  def testSampleSentences {

    val conf = new SparkConf()
      .setAppName("SparkTestRunner")
      .setMaster("local[*]")
      .set("spark.io.compression.codec","org.apache.spark.io.LZ4CompressionCodec")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array(classOf[FixedSizeTreeSet[_]], classOf[CT2[_]]))
//      .set("spark.kryo.classesToRegister", "org.jobimtext.util.FixedSizeTreeSet,org.jobimtext.ct2.CT2")


    val sc = new SparkContext(conf);

//    val lines_in = sc.textFile("org.jobimtext.ct/src/test/files/samplesentences_2.txt").filter(_.nonEmpty).repartition(sc.defaultParallelism)
    val lines_in = sc.textFile("/Volumes/ExtendedHD/Users/stevo/Documents/corpora/simplewiki/simplewikipedia_sent_tok.txt").filter(_.nonEmpty).repartition(sc.defaultParallelism)

//    val lines_out =
//      SimSortTopN(10,false,
//        KLDivergenceRdcBy(
//          JoinBySharedFeaturesGrpBy(-1,
//            TopProbs(1000,
//              ct2.ProbsFromCT(
//                ct2.SumMarginalsCT(
//                  ct2.AggregateCT.classic(
//                    ClassicToCT(
//                      CooccurrenceWindow(3,lines_in)
//                    )
//                  )
//                )
//              )
//            )
//          )
//        )
//      )

//    val lines_out =
//      TakeTopN(10,true,true,
////        KLDivergenceRdcBy(
//        FreqSim(with_features = true,
//          JoinBySharedFeaturesGrpBy(-1,
//            TakeTopN(100, true, false,
//               ct2.sig.LMIFromCT(
//                  ct2.AggregateCT.classic(Ctconf.default,
//                    ClassicToCT(
////                      CooccurrenceWindow(100,lines_in)
//                      NgramWithHole(3,false,lines_in)
//                    )
//                  )
//                )
//            )
//          )
//        )
//      )
    //lines_out.saveAsTextFile("org.jobimtext.ct/local_data/samplesentences_kls");

    val ctconf = Ctconf(
      min_ndot1 = 2, // min occurrences jo
      min_n1dot = 2, // min occurrences bim
      min_n11 = 2, // min occurrences jo-bim
      max_odot1 = 1000000000, // max different occurrences jo
      min_odot1 = 1, // min different occurrences jo
      min_docs = 1, // min docs
      min_sig = Double.NegativeInfinity,
      topn_f = 1000, // take top n contexts per jo
      topn_s = 100, // take top n similar jos per jo
      min_sim = 2
    )

    val lines_out =
      TakeTopN(100,true,true,
        FreqSim(with_features = false,
          JoinBySharedFeaturesGrpBy(-1,
              ct2.sig.FreqFromCT(
                ct2.AggregateCT.classic(ctconf,
                  ClassicToCT(
                    NgramWithHole(3,false,lines_in)
                  )
                )
              )
            )
          )
      )

    lines_out.saveAsTextFile("/Volumes/ExtendedHD/Users/stevo/Workspaces/flink-test/flink_quickstart/spark");

//    lines_out.foreach(line => println(line));

    sc.stop();
  }

  def testArtificalData {

    val conf = new SparkConf()
      .setAppName("SparkTestRunner")
      .setMaster("local[*]")
      .set("spark.io.compression.codec","org.apache.spark.io.LZ4CompressionCodec")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array(classOf[FixedSizeTreeSet[_]], classOf[CT2[_]]))


    val prunconf = Ctconf(min_n11 = 1, min_n1dot = 1, min_ndot1 = 1,min_odot1 = 1)

    val sc = new SparkContext(conf);

//    val lines_in = sc.textFile("org.jobimtext.ct/src/test/files/artificial-ct.txt").filter(_.nonEmpty)
//    val lines_out = AggregateCT2(lines_in)
//    val lines_out = AggregateCT2.classic(lines_in)


    val lines_in = sc.textFile("org.jobimtext.ct/src/test/files/artificial-jb.txt").filter(_.nonEmpty)

//        val lines_out = ClassicToCT(lines_in)
//    val lines_out = ct2.AggregateCT(ClassicToCT(lines_in));
//    val lines_out = ct2.AggregateCT.classic(ClassicToCT(lines_in));
//    val lines_out = AggregateCT(2, ClassicToCT(lines_in));
//    val lines_out = ClassicToCT.classicToAggregatedCT2(lines_in)

//    val lines_in = sc.textFile("org.jobimtext.ct/src/test/files/artificial-jb-wfc.txt").filter(_.nonEmpty)
//    val lines_out = ClassicToCT.classicWordFeatureCountToAggregatedCT2(lines_in)

    val lines_out =
//      SimSortTopN(1,false,
//        KLDivergenceRdcBy(
//          JoinBySharedFeaturesGrpBy(-1,
////              JoinBySharedFeaturesCartesian(
//            TakeTopN(1,true,
//              ct2.ProbsFromCT(
//                Prune.pruneCT(prunconf.filterCT,
                  ct2.AggregateCT.classic(Ctconf.default,
                    ClassicToCT(lines_in)
                  )
//                )
//              )
//            )
//          )
//        )
//      )

//    //lines_out.saveAsTextFile("org.jobimtext.ct/local_data/testout");
//    lines_out.collect().foreach(line => println(line));
    lines_out.takeSample(withReplacement = false, num = 100, seed = 42l).foreach(line => println(line))

    sc.stop();
  }

}
