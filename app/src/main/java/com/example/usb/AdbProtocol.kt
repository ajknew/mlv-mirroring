package com.example.usb

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AdbProtocol {
    const val HEADER_LENGTH = 24

    const val A_SYNC = 0x434e5953
    const val A_CNXN = 0x4e584e43
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257
    const val A_AUTH = 0x48545541

    const val AUTH_TYPE_TOKEN = 1
    const val AUTH_TYPE_SIGNATURE = 2
    const val AUTH_TYPE_RSA_PUBLIC = 3

    const val CONNECT_VERSION = 0x01000000
    const val CONNECT_MAXDATA = 4096

    const val ADB_CLASS = 255
    const val ADB_SUBCLASS = 66
    const val ADB_PROTOCOL = 1

    data class Message(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val length: Int,
        val checksum: Int,
        val magic: Int,
        val payload: ByteArray? = null
    ) {
        fun isValid(): Boolean {
            return (command xor -0x1) == magic
        }

        fun commandName(): String = when (command) {
            A_SYNC -> "SYNC"
            A_CNXN -> "CNXN"
            A_OPEN -> "OPEN"
            A_OKAY -> "OKAY"
            A_CLSE -> "CLSE"
            A_WRTE -> "WRTE"
            A_AUTH -> "AUTH"
            else -> "UNKNOWN(0x${Integer.toHexString(command)})"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Message
            return command == other.command &&
                    arg0 == other.arg0 &&
                    arg1 == other.arg1 &&
                    length == other.length &&
                    checksum == other.checksum &&
                    magic == other.magic &&
                    (payload?.contentEquals(other.payload) ?: (other.payload == null))
        }

        override fun hashCode(): Int {
            var result = command
            result = 31 * result + arg0
            result = 31 * result + arg1
            result = 31 * result + length
            result = 31 * result + checksum
            result = 31 * result + magic
            result = 31 * result + (payload?.contentHashCode() ?: 0)
            return result
        }
    }

    fun calculateChecksum(data: ByteArray?): Int {
        if (data == null || data.isEmpty()) return 0
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    fun createMessage(command: Int, arg0: Int, arg1: Int, payload: ByteArray?): Message {
        val length = payload?.size ?: 0
        val checksum = calculateChecksum(payload)
        val magic = command xor -0x1
        return Message(command, arg0, arg1, length, checksum, magic, payload)
    }

    fun serializeMessage(message: Message): ByteArray {
        val totalSize = HEADER_LENGTH + (message.payload?.size ?: 0)
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(message.command)
        buffer.putInt(message.arg0)
        buffer.putInt(message.arg1)
        buffer.putInt(message.length)
        buffer.putInt(message.checksum)
        buffer.putInt(message.magic)
        message.payload?.let { buffer.put(it) }
        return buffer.array()
    }

    fun parseHeader(headerBytes: ByteArray): Message {
        require(headerBytes.size >= HEADER_LENGTH) { "Header must be at least 24 bytes" }
        val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.getInt()
        val arg0 = buffer.getInt()
        val arg1 = buffer.getInt()
        val length = buffer.getInt()
        val checksum = buffer.getInt()
        val magic = buffer.getInt()
        return Message(command, arg0, arg1, length, checksum, magic)
    }
}
