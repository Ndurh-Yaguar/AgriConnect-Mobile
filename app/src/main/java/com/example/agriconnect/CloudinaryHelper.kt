package com.example.agriconnect

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Utility object for uploading images and files to Cloudinary.
 * Works for both Auction uploads and Product uploads.
 */
object CloudinaryUploadHelper {

    // --- Your Cloudinary credentials ---
    private const val CLOUD_NAME = "drcurjvsw"
    private const val UPLOAD_PRESET = "agriconnect_upload"

    /**
     * Uploads a file to Cloudinary using a given public ID.
     * Returns a Result<String> with the secure URL on success.
     */
    suspend fun uploadFile(context: Context, fileUri: Uri, publicId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes()
                    ?: return@withContext Result.failure(Exception("Failed to read file."))
                inputStream.close()

                val url = URL("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                val boundary = "Boundary-${System.currentTimeMillis()}"

                val connection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                val outputStream = DataOutputStream(connection.outputStream)

                // Upload preset
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                outputStream.writeBytes("$UPLOAD_PRESET\r\n")

                // Public ID
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"public_id\"\r\n\r\n")
                outputStream.writeBytes("$publicId\r\n")

                // File data
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"\r\n"
                )
                outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                outputStream.write(bytes)
                outputStream.writeBytes("\r\n--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val responseStream =
                    if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = responseStream.bufferedReader().use { it.readText() }

                if (responseCode == 200) {
                    // Extract secure_url from Cloudinary response
                    val secureUrl = Regex("\"secure_url\"\\s*:\\s*\"(.*?)\"")
                        .find(responseText)?.groupValues?.get(1)
                        ?.replace("\\/", "/")

                    if (secureUrl != null) {
                        Result.success(secureUrl)
                    } else {
                        Result.failure(Exception("Upload succeeded but URL not found in response."))
                    }
                } else {
                    Result.failure(Exception("Upload failed ($responseCode): $responseText"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Simpler overload that auto-generates a random public ID.
     * Used by ProductUpload.kt and any general uploads.
     */
    suspend fun uploadFile(context: Context, fileUri: Uri): Result<String> {
        val publicId = UUID.randomUUID().toString()
        return uploadFile(context, fileUri, publicId)
    }
}
