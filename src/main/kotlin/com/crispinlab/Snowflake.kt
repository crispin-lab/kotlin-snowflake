package com.crispinlab

import java.net.NetworkInterface
import java.security.SecureRandom
import java.time.Instant
import java.util.Enumeration


class Snowflake(
    private val nodeId: Long,
    private val customEpoch: Long
) {
    init {
        require(
            nodeId in 1.rangeTo(MAX_NODE_ID)
        ) { "NodeId must be between ${0} and $MAX_NODE_ID" }
    }

    companion object {
        @Suppress("unused")
        private const val UNUSED_SIGN_BIT: Int = 1
        private const val EPOCH_BITS: Int = 41
        private const val NODE_ID_BITS: Int = 10
        private const val SEQUENCE_BITS: Int = 12
        private const val MAX_NODE_ID: Long = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE: Long = (1L shl SEQUENCE_BITS) - 1

        // Default Epoch (2025-01-01T00:00:00Z)
        private const val DEFAULT_EPOCH: Long = 1735689600000L

        private fun createNodeId(): Long {
            var nodeId: Int
            try {
                val stringBuilder = StringBuilder()
                val networkInterfaces: Enumeration<NetworkInterface> =
                    NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface: NetworkInterface = networkInterfaces.nextElement()
                    val mac: ByteArray? = networkInterface.hardwareAddress
                    if (mac != null) {
                        for (macPort: Byte in mac) {
                            stringBuilder.append(macPort)
                        }
                    }
                }
                nodeId = stringBuilder.toString().hashCode()
            } catch (e: Exception) {
                nodeId = SecureRandom().nextInt()
            }
            return nodeId.toLong() and MAX_NODE_ID
        }
    }

    @Volatile
    private var lastTimestamp: Long = -1L

    @Volatile
    private var sequence: Long = 0L

    constructor(nodeId: Long) : this(nodeId, customEpoch = DEFAULT_EPOCH)
    constructor() : this(nodeId = createNodeId(), customEpoch = DEFAULT_EPOCH)

    @Synchronized
    fun nextId(): Long {
        var currentTimestamp: Long = timestamp()

        require(currentTimestamp >= lastTimestamp) {
            "Invalid System Clock."
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                currentTimestamp = waitNextMillis(currentTimestamp)
            }
        } else {
            sequence = 0
        }

        lastTimestamp = currentTimestamp

        return currentTimestamp shl
            (NODE_ID_BITS + SEQUENCE_BITS) or
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

    fun parse(id: Long): Array<Long> {
        val maskNodeId: Long = ((1L shl NODE_ID_BITS) - 1) shl SEQUENCE_BITS
        val maskSequence: Long = (1L shl SEQUENCE_BITS) - 1

        val timestamp: Long = (id shr (NODE_ID_BITS + SEQUENCE_BITS)) + customEpoch
        val nodeId: Long = (id and maskNodeId) shr SEQUENCE_BITS
        val sequence: Long = id and maskSequence

        return arrayOf(timestamp, nodeId, sequence)
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
