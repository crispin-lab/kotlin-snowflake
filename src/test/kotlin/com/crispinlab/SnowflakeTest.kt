package com.crispinlab

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test

class SnowflakeTest {
    @Test
    fun generateSnowflakeNextIdTest() {
        // given
        val sequence = 0L
        val nodeId = 777L
        val snowflake = Snowflake(nodeId)
        val beforeTimestamp: Long = Instant.now().toEpochMilli()

        // when
        val id: Long = snowflake.nextId()

        // then
        val parses: Array<Long> = snowflake.parse(id)
        SoftAssertions.assertSoftly {
            it.assertThat(parses[0]).isGreaterThanOrEqualTo(beforeTimestamp)
            it.assertThat(parses[1]).isEqualTo(nodeId)
            it.assertThat(parses[2]).isEqualTo(sequence)
        }
    }

    @Test
    fun generateSnowflakeUniqueIdTest() {
        // given
        val nodeId = 777L
        val iterations = 7777
        val ids = LongArray(iterations)
        val snowflake = Snowflake(nodeId)

        // when
        for (i: Int in 0..<iterations) {
            ids[i] = snowflake.nextId()
        }

        // then
        val uniqueIds: Set<Long> = ids.toSet()
        Assertions.assertThat(uniqueIds).hasSize(iterations)
    }

    @Test
    fun generateMultiThreadSnowflakeUniqueIdTest() {
        // given
        val nodeId = 777L
        val threadCount = 50
        val iterations = 100000
        val snowflake = Snowflake(nodeId)
        val latch = CountDownLatch(threadCount)
        val futures: Array<Future<Long>?> = arrayOfNulls(iterations)
        val executorService: ExecutorService = Executors.newFixedThreadPool(threadCount)

        // when
        for (i: Int in 0..<iterations) {
            futures[i] =
                executorService.submit<Long> {
                    val id: Long = snowflake.nextId()
                    latch.countDown()
                    id
                }
        }

        latch.await()

        // then
        Assertions.assertThat(futures.map { it!!.get() }.toSet()).hasSize(iterations)
        executorService.shutdown()
    }

    @Test
    fun generateSnowflakeFailTest() {
        // given
        val wrongNodeId = -1L
        val nodeIdBits = 10
        val maxNodeId: Long = (1L shl nodeIdBits) - 1

        // when & then
        Assertions
            .assertThatThrownBy {
                Snowflake(wrongNodeId)
            }.hasMessage(
                "NodeId must be between ${0} and $maxNodeId"
            )
    }

    @Test
    fun generateNodeIdTest() {
        // given
        val nodeIdIndex = 1

        // when
        val snowflake = Snowflake()

        // then
        val nodeId: Long = snowflake.parse(snowflake.nextId())[nodeIdIndex]
        SoftAssertions.assertSoftly {
            it.assertThat(nodeId).isNotNull
            it.assertThat(nodeId).isGreaterThan(0)
        }
    }

    @Test
    fun snowflakeTimeOrderTest() {
        // given
        val nodeId = 777L
        val snowflake = Snowflake(nodeId)

        // when & then
        var prevId: Long = snowflake.nextId()
        for (i: Int in 0..<1000) {
            val currentId: Long = snowflake.nextId()
            Assertions.assertThat(currentId).isGreaterThan(prevId)
            prevId = currentId
        }
    }
}
