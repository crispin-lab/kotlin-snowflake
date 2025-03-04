package com.crispinlab

import java.net.NetworkInterface
import java.security.SecureRandom
import java.time.Instant
import java.util.Enumeration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Distributed Unique ID Generator using Snowflake algorithm
 * Structure:
 * - 1 bit: Unused (sign bit)
 * - 41 bits: Timestamp (milliseconds since epoch)
 * - 10 bits: Node ID
 * - 12 bits: Sequence number
 */
class Snowflake private constructor(
    private val nodeId: Long,
    private val customEpoch: Long
) {
    private val lock = ReentrantLock()
    private var lastTimestamp: Long = -1L
    private var sequence: Long = 0L

    companion object {
        private const val EPOCH_BITS: Int = 41
        private const val NODE_ID_BITS: Int = 10
        private const val SEQUENCE_BITS: Int = 12
        private const val MAX_NODE_ID: Long = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE: Long = (1L shl SEQUENCE_BITS) - 1

        // Default Epoch (2025-01-01T00:00:00Z)
        private const val DEFAULT_EPOCH: Long = 1735689600000L

        /**
         * Creates a Snowflake generator with automatically derived node ID and default epoch.
         * @return Snowflake generator instance with automatically derived node ID and default epoch.
         */
        fun create() = Snowflake(nodeId = createNodeId(), customEpoch = DEFAULT_EPOCH)

        /**
         * Creates a Snowflake generator with the specified node ID and default epoch
         * @param nodeId specified node ID (a value greater than 0)
         * @throws IllegalStateException if the node ID is less than or equal to 0
         * @return Snowflake generator instance with specified node ID and default epoch
         */
        fun create(nodeId: Long): Snowflake {
            require(nodeId in 0..MAX_NODE_ID) {
                "NodeId must be between 0 and $MAX_NODE_ID"
            }
            return Snowflake(nodeId, DEFAULT_EPOCH)
        }

        /**
         * Creates a Snowflake generator with the specified node ID and custom epoch
         * @param nodeId specified node ID (a value greater than 0)
         * @param customEpoch custom epoch
         * @throws IllegalStateException if the node ID is less than or equal to 0
         * @return Snowflake generator instance with specified node ID and custom epoch
         */
        fun create(
            nodeId: Long,
            customEpoch: Long
        ): Snowflake {
            require(nodeId in 0..MAX_NODE_ID) {
                "NodeId must be between 0 and $MAX_NODE_ID"
            }
            return Snowflake(nodeId, customEpoch)
        }

        private fun createNodeId(): Long =
            try {
                val stringBuilder = StringBuilder()
                val networkInterfaces: Enumeration<NetworkInterface> =
                    NetworkInterface.getNetworkInterfaces()

                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface: NetworkInterface = networkInterfaces.nextElement()
                    networkInterface.hardwareAddress?.let { mac ->
                        mac.forEach { stringBuilder.append(it) }
                    }
                }

                if (stringBuilder.isEmpty()) {
                    SecureRandom().nextLong() and MAX_NODE_ID
                } else {
                    stringBuilder.toString().hashCode().toLong() and MAX_NODE_ID
                }
            } catch (exception: Exception) {
                SecureRandom().nextLong() and MAX_NODE_ID
            }
    }

    /**
     * Generates a new unique ID
     * @return a unique snowflake ID
     */
    fun nextId(): Long =
        lock.withLock {
            var currentTimestamp: Long = timestamp()

            when {
                currentTimestamp < lastTimestamp -> throw IllegalStateException(
                    "Clock moved backwards. Refusing to generate ID."
                )

                currentTimestamp == lastTimestamp -> {
                    sequence = (sequence + 1) and MAX_SEQUENCE
                    if (sequence == 0L) {
                        currentTimestamp = waitNextMillis(currentTimestamp)
                    }
                }

                else -> sequence = 0L
            }

            lastTimestamp = currentTimestamp
            return@withLock (currentTimestamp shl (NODE_ID_BITS + SEQUENCE_BITS)) or
                (nodeId shl SEQUENCE_BITS) or
                sequence
        }

    private fun timestamp(): Long = Instant.now().toEpochMilli() - customEpoch

    private fun waitNextMillis(currentTimestamp: Long): Long {
        var timestamp: Long = currentTimestamp

        while (timestamp == lastTimestamp) {
            timestamp = timestamp()
        }
        return timestamp
    }

    /**
     * Parses a snowflake ID into its components
     * @param id the snowflake ID to parse
     * @return SnowflakeComponents instance with node ID, timestamp, sequence, toInstant()
     */
    fun parse(id: Long): SnowflakeComponents {
        val maskNodeId: Long = ((1L shl NODE_ID_BITS) - 1) shl SEQUENCE_BITS
        val maskSequence: Long = (1L shl SEQUENCE_BITS) - 1
        val timestamp: Long = (id shr (NODE_ID_BITS + SEQUENCE_BITS)) + customEpoch
        val nodeId: Long = (id and maskNodeId) shr SEQUENCE_BITS
        val sequence: Long = id and maskSequence

        return SnowflakeComponents(nodeId, timestamp, sequence)
    }

    /**
     * Data class to hold parsed snowflake components
     */
    data class SnowflakeComponents(
        val nodeId: Long,
        val timestamp: Long,
        val sequence: Long
    ) {
        /**
         * Converts an epoch time to an Instant
         * @return An Instant representing the specified epoch time
         */
        fun toInstant(): Instant = Instant.ofEpochMilli(timestamp)
    }

    override fun toString(): String =
        "Snowflake Settings [" +
            "EPOCH_BITS=$EPOCH_BITS, " +
            "NODE_ID_BITS=$NODE_ID_BITS, " +
            "SEQUENCE_BITS=$SEQUENCE_BITS, " +
            "CUSTOM_EPOCH=$customEpoch, " +
            "NodeId=$nodeId" +
            "]"
}
