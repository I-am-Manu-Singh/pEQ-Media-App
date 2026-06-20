package org.audiocular.peq.app

import java.io.File
import java.io.FileInputStream

object Mp4CoverExtractor {
    private fun skipExactly(input: FileInputStream, n: Long) {
        var remaining = n
        val buffer = ByteArray(4096)
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                // Fallback to reading if skip returns 0 or negative
                val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read <= 0) break
                remaining -= read
            } else {
                remaining -= skipped
            }
        }
    }

    fun extract(file: File): ByteArray? {
        try {
            if (!file.exists()) return null
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8)
                while (input.read(buffer) == 8) {
                    val size = ((buffer[0].toInt() and 0xFF) shl 24) or
                               ((buffer[1].toInt() and 0xFF) shl 16) or
                               ((buffer[2].toInt() and 0xFF) shl 8) or
                               (buffer[3].toInt() and 0xFF)
                    val type = String(buffer, 4, 4)
                    
                    val boxHeaderSize = if (size == 1) 16 else 8
                    val payloadSize = if (size == 1) {
                        val sizeBuf = ByteArray(8)
                        if (input.read(sizeBuf) != 8) return null
                        val s = ((sizeBuf[0].toLong() and 0xFF) shl 56) or
                                ((sizeBuf[1].toLong() and 0xFF) shl 48) or
                                ((sizeBuf[2].toLong() and 0xFF) shl 40) or
                                ((sizeBuf[3].toLong() and 0xFF) shl 32) or
                                ((sizeBuf[4].toLong() and 0xFF) shl 24) or
                                ((sizeBuf[5].toLong() and 0xFF) shl 16) or
                                ((sizeBuf[6].toLong() and 0xFF) shl 8) or
                                (sizeBuf[7].toLong() and 0xFF)
                        s - boxHeaderSize
                    } else {
                        (size - boxHeaderSize).toLong()
                    }
                    
                    if (type == "moov" || type == "udta" || type == "meta" || type == "ilst" || type == "covr") {
                        if (type == "meta") {
                            skipExactly(input, 4) // Skip the 4-byte version/flags field in the meta box
                        }
                        continue
                    } else if (type == "data" && payloadSize > 8) {
                        skipExactly(input, 8) // Skip the type indicator and locale indicators
                        val dataSize = (payloadSize - 8).toInt()
                        val data = ByteArray(dataSize)
                        var read = 0
                        while (read < dataSize) {
                            val r = input.read(data, read, dataSize - read)
                            if (r <= 0) break
                            read += r
                        }
                        return data
                    } else {
                        if (payloadSize > 0) {
                            skipExactly(input, payloadSize)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("pEQControl: custom MP4 extractor error: ${e.message}")
        }
        return null
    }
}
