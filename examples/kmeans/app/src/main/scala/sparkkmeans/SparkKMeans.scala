/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import Array._
import scala.math._
import org.apache.spark.rdd._
import java.net._

/**
 * K-means clustering.
 */
object SparkKMeans {

  def squaredDistance(p: Array[Double], q: Array[Double]): Double = {
    var tempDist: Double = 0
    var j: Int = 0
    while (j < p.length) {
      tempDist += (p(j) - q(j)) * (p(j) - q(j))
      j += 1
    }
    tempDist
  }

  def main(args: Array[String]) {

    if (args.length < 3) {
      System.err.println("Usage: SparkKMeans <file> <k> <convergeDist>")
      System.exit(1)
    }

    val sparkConf = new SparkConf().setAppName("KMeans")
    val spark = new SparkContext(sparkConf)

    val lines = spark.textFile(args(0))
    val data = lines.map(l => l.split(' ').map(e => e.toDouble)).cache()
    val K = args(1).toInt
    val convergeDist = args(2).toDouble

    val kPoints = data.takeSample(withReplacement = false, K, 42)
    var tempDist = 1.0

    while(tempDist > convergeDist) {
      val flatKPoints = kPoints.flatMap(e => e)
      val closestIdx = data.map(p => {
        var bestIndex = 0
        var closest = 1e10

        var i: Int = 0
        while (i < 3) {
          var dist = 0.0
          var j: Int = 0
          while (j < 12) {
            dist += (p(j) - flatKPoints(i * 12 + j)) * (p(j) - flatKPoints(i * 12 + j))
            j += 1
          }
          if (dist < closest) {
            closest = dist
            bestIndex = i
          }
          i += 1
        }
        bestIndex
      })

      val closest = closestIdx.zip(data).map{case (i, p) => (i, (p, 1))}

      val pointStats = closest.reduceByKey{case ((p1, c1), (p2, c2)) =>
        (p1.zip(p2).map{case (x, y) => x + y}, c1 + c2)
      }

      val newPoints = pointStats.map {pair =>
        (pair._1, pair._2._1.map(_ * (1.0 / pair._2._2)))}.collectAsMap()

      tempDist = 0.0
      for (i <- 0 until K) {
        tempDist += squaredDistance(kPoints(i), newPoints(i))
      }

      for (newP <- newPoints) {
        kPoints(newP._1) = newP._2
      }
      println("Finished iteration (delta = " + tempDist + ")")
    }

    spark.stop()
  }
}
// scalastyle:on println
