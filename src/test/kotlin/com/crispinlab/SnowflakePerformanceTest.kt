package com.crispinlab

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.junit.jupiter.api.Test

class SnowflakePerformanceTest {
    @Test
    fun snowflakeNextIdGenerationWithSingleThreadTest() {
        // given
        val nodeId = 777L
        val iterations = 1000000
        val snowflake = Snowflake(nodeId)
        val start: Long = System.currentTimeMillis()

        // when
        for (i: Int in 0..<iterations) {
            val nextId = snowflake.nextId()
            println(nextId)
        }

        // then
        val end: Long = System.currentTimeMillis()
        val time: Long = iterations / (end - start)
        println("Single Thread -> IDs generate per ms: $time")
    }

    @Test
    fun snowflakeNextIdGenerationWithMultiThreadTest() {
        // given
        val nodeId = 777L
        val threadCount = 50
        val iterations = 1000000
        val snowflake = Snowflake(nodeId)
        val latch = CountDownLatch(threadCount)
        val futures: Array<Future<Long>?> = arrayOfNulls(iterations)
        val executorService: ExecutorService = Executors.newFixedThreadPool(threadCount)
        val start: Long = System.currentTimeMillis()
        val tmp: ArrayList<Long> = ArrayList()

        // when
        for (i: Int in 0..<iterations) {
            futures[i] =
                executorService.submit<Long> {
                    val nextId = snowflake.nextId()
                    latch.countDown()
                    nextId
                }
        }

        latch.await()
        futures.map { tmp.add(it!!.get()) }

        println(tmp.size)

        // then
        val end: Long = System.currentTimeMillis()
        val time: Long = iterations / (end - start)
        println("Single Thread -> IDs generate per ms: $time")
        executorService.shutdown()
    }
}
