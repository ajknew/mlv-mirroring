package com.example

import com.example.usb.AdbProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdbProtocolUnitTest {

    @Test
    fun `createMessage calculates correct length and magic`() {
        val payload = "host::mlv_mirroring\u0000".toByteArray(Charsets.UTF_8)
        val msg = AdbProtocol.createMessage(
            AdbProtocol.A_CNXN,
            AdbProtocol.CONNECT_VERSION,
            AdbProtocol.CONNECT_MAXDATA,
            payload
        )

        assertEquals(AdbProtocol.A_CNXN, msg.command)
        assertEquals(AdbProtocol.CONNECT_VERSION, msg.arg0)
        assertEquals(AdbProtocol.CONNECT_MAXDATA, msg.arg1)
        assertEquals(payload.size, msg.length)
        assertEquals(AdbProtocol.A_CNXN xor -1, msg.magic)
        assertTrue(msg.isValid())
    }

    @Test
    fun `checksum matches byte sum`() {
        val data = byteArrayOf(10, 20, 30, 40)
        val expectedSum = 10 + 20 + 30 + 40
        assertEquals(expectedSum, AdbProtocol.calculateChecksum(data))
    }

    @Test
    fun `serialization and header deserialization are reversible`() {
        val payload = "exec:screenrecord --output-format=h264 -\u0000".toByteArray(Charsets.UTF_8)
        val original = AdbProtocol.createMessage(AdbProtocol.A_OPEN, 1, 0, payload)

        val serialized = AdbProtocol.serializeMessage(original)
        assertTrue(serialized.size >= AdbProtocol.HEADER_LENGTH + payload.size)

        val headerBytes = serialized.copyOfRange(0, AdbProtocol.HEADER_LENGTH)
        val parsed = AdbProtocol.parseHeader(headerBytes)

        assertEquals(original.command, parsed.command)
        assertEquals(original.arg0, parsed.arg0)
        assertEquals(original.arg1, parsed.arg1)
        assertEquals(original.length, parsed.length)
        assertEquals(original.checksum, parsed.checksum)
        assertEquals(original.magic, parsed.magic)
        assertTrue(parsed.isValid())
    }
}
