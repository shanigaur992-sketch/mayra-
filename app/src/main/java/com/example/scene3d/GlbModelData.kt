package com.example.scene3d

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GlbModelInfo(
    val fileName: String,
    val fileSize: Long,
    val animationNames: List<String>,
    val meshCount: Int,
    val nodeCount: Int,
    val materialCount: Int,
    val isGlbBinary: Boolean
)

object GlbParser {

    private const val GLB_MAGIC = 0x46546C67 // "glTF" in little-endian

    fun parseGlbHeader(inputStream: InputStream, fileName: String, fileSize: Long): GlbModelInfo {
        val headerBytes = ByteArray(12)
        val read = inputStream.read(headerBytes)
        if (read < 12) {
            return GlbModelInfo(fileName, fileSize, emptyList(), 0, 0, 0, false)
        }

        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = headerBuffer.int
        val version = headerBuffer.int
        val length = headerBuffer.int

        if (magic != GLB_MAGIC) {
            // Not GLB binary; could be plain JSON .gltf
            return GlbModelInfo(fileName, fileSize, listOf("ProceduralFallback"), 1, 1, 1, false)
        }

        // Read first chunk (JSON chunk)
        val chunkHeader = ByteArray(8)
        inputStream.read(chunkHeader)
        val chunkBuf = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN)
        val chunkLength = chunkBuf.int
        val chunkType = chunkBuf.int // 0x4E4F534A ("JSON")

        val jsonBytes = ByteArray(chunkLength.coerceAtMost(2 * 1024 * 1024)) // Limit reading to 2MB json
        inputStream.read(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)

        return parseGltfJson(jsonString, fileName, fileSize, isGlb = true)
    }

    fun parseGltfJson(jsonString: String, fileName: String, fileSize: Long, isGlb: Boolean): GlbModelInfo {
        return try {
            val root = JSONObject(jsonString)
            val animations = mutableListOf<String>()
            val animArray = root.optJSONArray("animations")
            if (animArray != null) {
                for (i in 0 until animArray.length()) {
                    val animObj = animArray.optJSONObject(i)
                    val name = animObj?.optString("name")
                    if (!name.isNullOrBlank()) {
                        animations.add(name)
                    } else {
                        animations.add("Animation_${i + 1}")
                    }
                }
            }

            val meshCount = root.optJSONArray("meshes")?.length() ?: 0
            val nodeCount = root.optJSONArray("nodes")?.length() ?: 0
            val materialCount = root.optJSONArray("materials")?.length() ?: 0

            GlbModelInfo(
                fileName = fileName,
                fileSize = fileSize,
                animationNames = animations,
                meshCount = meshCount,
                nodeCount = nodeCount,
                materialCount = materialCount,
                isGlbBinary = isGlb
            )
        } catch (_: Exception) {
            GlbModelInfo(fileName, fileSize, emptyList(), 0, 0, 0, isGlb)
        }
    }

    fun copyGlbToInternalStorage(context: Context, uri: Uri): File {
        val destFile = File(context.filesDir, "myraa_primary_avatar.glb")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }
}
