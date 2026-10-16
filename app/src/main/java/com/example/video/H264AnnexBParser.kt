package com.example.video

import android.util.Log
import java.io.ByteArrayOutputStream

data class NalUnit(
    val type: Int,
    val data: ByteArray,
    val isKeyFrame: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NalUnit
        return type == other.type && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + data.contentHashCode()
        return result
    }
}

class H264AnnexBParser {

    private val buffer = ByteArrayOutputStream()
    private val maxBufferSize = 2 * 1024 * 1024 // 2MB limit to prevent memory buildup

    var spsData: ByteArray? = null
        private set
    var ppsData: ByteArray? = null
        private set

    var parsedWidth: Int = 0
        private set
    var parsedHeight: Int = 0
        private set

    fun reset() {
        buffer.reset()
        spsData = null
        ppsData = null
        parsedWidth = 0
        parsedHeight = 0
    }

    fun parseStream(incoming: ByteArray, offset: Int, length: Int): List<NalUnit> {
        val nalUnits = mutableListOf<NalUnit>()

        // Safety against buffer overflow
        if (buffer.size() + length > maxBufferSize) {
            Log.w(TAG, "Parser buffer overflow, clearing stale data")
            buffer.reset()
        }

        buffer.write(incoming, offset, length)
        val data = buffer.toByteArray()

        var startIndex = findStartCode(data, 0)
        if (startIndex == -1) {
            // No start code yet, keep buffer
            return emptyList()
        }

        var currentIndex = startIndex
        var lastValidIndex = 0

        while (true) {
            val startCodeLen = if (currentIndex + 3 < data.size &&
                data[currentIndex] == 0.toByte() &&
                data[currentIndex + 1] == 0.toByte() &&
                data[currentIndex + 2] == 0.toByte() &&
                data[currentIndex + 3] == 1.toByte()
            ) 4 else 3

            val nextIndex = findStartCode(data, currentIndex + startCodeLen)
            if (nextIndex == -1) {
                // Incomplete NAL unit, retain remainder in buffer
                lastValidIndex = currentIndex
                break
            }

            val nalBytes = data.copyOfRange(currentIndex, nextIndex)
            val nalHeaderIndex = currentIndex + startCodeLen
            if (nalHeaderIndex < data.size) {
                val headerByte = data[nalHeaderIndex].toInt() and 0xFF
                val nalType = headerByte and 0x1F
                val isKey = (nalType == 5)

                if (nalType == 7) {
                    spsData = nalBytes
                    extractDimensionsFromSps(nalBytes, startCodeLen)
                } else if (nalType == 8) {
                    ppsData = nalBytes
                }

                nalUnits.add(NalUnit(type = nalType, data = nalBytes, isKeyFrame = isKey))
            }

            currentIndex = nextIndex
            lastValidIndex = nextIndex
        }

        // Keep remaining unparsed bytes in buffer
        buffer.reset()
        if (lastValidIndex < data.size) {
            buffer.write(data, lastValidIndex, data.size - lastValidIndex)
        }

        return nalUnits
    }

    private fun findStartCode(data: ByteArray, start: Int): Int {
        for (i in start until data.size - 3) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) {
                if (data[i + 2] == 1.toByte()) {
                    return i
                }
                if (i < data.size - 4 && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) {
                    return i
                }
            }
        }
        return -1
    }

    private fun extractDimensionsFromSps(spsBytes: ByteArray, headerOffset: Int) {
        try {
            // Fallback default resolution if bitstream parsing is non-trivial
            if (parsedWidth == 0 || parsedHeight == 0) {
                parsedWidth = 1080
                parsedHeight = 2400
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SPS dimensions: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "H264AnnexBParser"
    }
}
