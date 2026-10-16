package com.example

import com.example.video.H264AnnexBParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class H264ParserUnitTest {

    private lateinit var parser: H264AnnexBParser

    @Before
    fun setUp() {
        parser = H264AnnexBParser()
    }

    @Test
    fun `detects 3-byte and 4-byte Annex B start codes and extracts NAL types`() {
        // Stream: [0 0 0 1, SPS(7), data], [0 0 0 1, PPS(8), data], [0 0 1, IDR(5), data]
        val fakeStream = byteArrayOf(
            0, 0, 0, 1, 0x67.toByte(), 0x42, 0x00, 0x1f, // SPS (type 7: 0x67 & 0x1F = 7)
            0, 0, 0, 1, 0x68.toByte(), 0xce.toByte(), 0x3c, 0x80.toByte(), // PPS (type 8: 0x68 & 0x1F = 8)
            0, 0, 1, 0x65.toByte(), 0x88.toByte(), 0x84.toByte(), // IDR (type 5: 0x65 & 0x1F = 5)
            0, 0, 0, 1, 0x41.toByte(), 0x9a.toByte() // Non-IDR (type 1: 0x41 & 0x1F = 1)
        )

        val nalUnits = parser.parseStream(fakeStream, 0, fakeStream.size)

        assertEquals(3, nalUnits.size) // First 3 full NAL units parsed (last one waiting for next boundary)
        assertEquals(7, nalUnits[0].type)
        assertEquals(8, nalUnits[1].type)
        assertEquals(5, nalUnits[2].type)
        assertTrue(nalUnits[2].isKeyFrame)

        assertNotNull(parser.spsData)
        assertNotNull(parser.ppsData)
    }

    @Test
    fun `incomplete stream chunks buffer until completion`() {
        val part1 = byteArrayOf(0, 0, 0, 1, 0x67.toByte(), 0x11)
        val part2 = byteArrayOf(0x22, 0x33, 0, 0, 0, 1, 0x68.toByte(), 0x44)

        val res1 = parser.parseStream(part1, 0, part1.size)
        assertEquals(0, res1.size) // Waiting for end delimiter

        val res2 = parser.parseStream(part2, 0, part2.size)
        assertEquals(1, res2.size) // Completed first NAL unit
        assertEquals(7, res2[0].type)
    }

    @Test
    fun `reset clears buffer and SPS-PPS caches`() {
        val stream = byteArrayOf(
            0, 0, 0, 1, 0x67.toByte(), 0x12,
            0, 0, 0, 1, 0x68.toByte(), 0x34
        )
        parser.parseStream(stream, 0, stream.size)
        assertNotNull(parser.spsData)

        parser.reset()
        assertEquals(null, parser.spsData)
        assertEquals(null, parser.ppsData)
    }
}
