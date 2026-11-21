package com.factoria.core.blueprint

import com.factoria.core.model.*
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Inflater

sealed class BlueprintDecodeResult {
    data class Success(val wrapper: BlueprintStringWrapper, val rawJson: String) : BlueprintDecodeResult()
    data class Failure(val error: BlueprintError, val message: String) : BlueprintDecodeResult()
}

enum class BlueprintError {
    EMPTY_STRING,
    INVALID_VERSION_BYTE,
    INVALID_BASE64,
    DECOMPRESSION_FAILED,
    PARSING_FAILED,
    OVERSIZED_PAYLOAD
}

object BlueprintDecoder {
    private const val MAX_UNCOMPRESSED_SIZE_BYTES = 50 * 1024 * 1024 // 50MB safety limit for mega-books

    private class OversizedPayloadException(message: String) : Exception(message)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun decode(
        blueprintString: String,
        maxUncompressedSizeBytes: Int = MAX_UNCOMPRESSED_SIZE_BYTES
    ): BlueprintDecodeResult {
        val trimmed = blueprintString.trim()
        if (trimmed.isEmpty()) {
            return BlueprintDecodeResult.Failure(BlueprintError.EMPTY_STRING, "Blueprint string is empty")
        }

        // Factorio blueprint strings start with a version byte, currently '0'
        val firstChar = trimmed[0]
        val base64Payload = if (firstChar == '0') {
            trimmed.substring(1)
        } else {
            // Check if it's already pure base64
            trimmed
        }

        val compressedBytes = try {
            Base64.getDecoder().decode(base64Payload)
        } catch (e: IllegalArgumentException) {
            return BlueprintDecodeResult.Failure(
                BlueprintError.INVALID_BASE64,
                "Invalid Base64 encoding: ${e.localizedMessage}"
            )
        }

        val jsonString = try {
            decompressZlib(compressedBytes, maxUncompressedSizeBytes)
        } catch (e: OversizedPayloadException) {
            return BlueprintDecodeResult.Failure(
                BlueprintError.OVERSIZED_PAYLOAD,
                e.message ?: "Decompressed blueprint exceeds maximum allowed size"
            )
        } catch (e: Exception) {
            return BlueprintDecodeResult.Failure(
                BlueprintError.DECOMPRESSION_FAILED,
                "Failed to decompress zlib payload: ${e.localizedMessage}"
            )
        }

        return try {
            val wrapper = jsonParser.decodeFromString<BlueprintStringWrapper>(jsonString)
            BlueprintDecodeResult.Success(wrapper, jsonString)
        } catch (e: Exception) {
            BlueprintDecodeResult.Failure(
                BlueprintError.PARSING_FAILED,
                "Invalid JSON or unsupported blueprint format: ${e.localizedMessage}"
            )
        }
    }

    private fun decompressZlib(compressed: ByteArray, maxUncompressedSizeBytes: Int): String {
        val inflater = Inflater()
        inflater.setInput(compressed)
        val outputStream = ByteArrayOutputStream(minOf(compressed.size * 2, 65_536))
        val buffer = ByteArray(4096)
        var totalBytes = 0

        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        throw IllegalArgumentException("Incomplete or corrupted zlib stream")
                    }
                }
                totalBytes += count
                if (totalBytes > maxUncompressedSizeBytes) {
                    throw OversizedPayloadException(
                        "Decompressed blueprint exceeds maximum allowed size ($maxUncompressedSizeBytes bytes)"
                    )
                }
                outputStream.write(buffer, 0, count)
            }
            return outputStream.toString(Charsets.UTF_8.name())
        } finally {
            inflater.end()
            outputStream.close()
        }
    }
}
