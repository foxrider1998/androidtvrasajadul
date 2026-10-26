package com.example.androidtvrasajadul.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility untuk meresolve URL YouTube ke HLS stream URL menggunakan Invidious / Piped API.
 * Menghasilkan HLS m3u8 stream langsung yang bisa diputar ExoPlayer tanpa iklan.
 */
object YouTubeResolver {

    private const val TAG = "YouTubeResolver"

    /**
     * Active Invidious & Piped public instances.
     */
    private val INVIDIOUS_INSTANCES = listOf(
        "https://yt.chocolatemoo53.com",
        "https://invidious.tiekoetter.com"
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com/watch") ||
               url.contains("youtu.be/") ||
               url.contains("youtube.com/@") ||
               url.contains("youtube.com/live")
    }

    fun extractVideoId(url: String): String? {
        return when {
            url.contains("watch?v=") -> {
                val start = url.indexOf("watch?v=") + 8
                val end = url.indexOf("&", start).takeIf { it > 0 } ?: url.length
                url.substring(start, end).trim().takeIf { it.isNotEmpty() }
            }
            url.contains("youtu.be/") -> {
                val start = url.indexOf("youtu.be/") + 9
                val end = url.indexOf("?", start).takeIf { it > 0 } ?: url.length
                url.substring(start, end).trim().takeIf { it.isNotEmpty() }
            }
            url.contains("youtube.com/live/") -> {
                val start = url.indexOf("youtube.com/live/") + 17
                val end = url.indexOf("?", start).takeIf { it > 0 } ?: url.length
                url.substring(start, end).trim().takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }

    suspend fun resolveHlsUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrl)
        if (videoId == null) {
            Log.e(TAG, "Gagal ekstrak video ID dari URL: $youtubeUrl")
            return@withContext null
        }

        Log.d(TAG, "Meresolve YouTube video ID: $videoId")

        // Try Invidious instances first (returns hlsUrl)
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val apiUrl = "$instance/api/v1/videos/$videoId"
                Log.d(TAG, "Mencoba Invidious instance: $apiUrl")

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().readText()
                    val obj = JSONObject(responseText)
                    val hlsUrl = obj.optString("hlsUrl", "")
                    if (hlsUrl.isNotEmpty() && hlsUrl.startsWith("http")) {
                        Log.d(TAG, "Berhasil resolve via Invidious $instance → $hlsUrl")
                        return@withContext hlsUrl
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious instance $instance gagal: ${e.message}")
            }
        }

        Log.e(TAG, "Gagal resolve video ID: $videoId")
        null
    }
}
