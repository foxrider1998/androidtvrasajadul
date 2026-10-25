package com.example.androidtvrasajadul.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility untuk meresolve URL YouTube ke HLS stream URL menggunakan Piped API.
 *
 * Piped adalah frontend YouTube open-source yang expose HLS stream URL secara langsung
 * sehingga ExoPlayer bisa memutar video YouTube TANPA iklan, TANPA browser/WebView.
 *
 * Flow:
 *  1. Ekstrak Video ID dari URL youtube.com/watch?v=XXX atau youtu.be/XXX
 *  2. Panggil Piped API: GET /streams/{videoId}
 *  3. Ambil field "hls" dari response JSON → URL m3u8 yang bisa diputar ExoPlayer
 *  4. Jika instance Piped gagal → coba instance fallback berikutnya
 */
object YouTubeResolver {

    private const val TAG = "YouTubeResolver"

    /**
     * Daftar Piped API public instances sebagai fallback.
     * Diurutkan dari yang paling stabil.
     */
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.yt",
        "https://piped-api.privacy.com.de",
        "https://pipedapi.in.projectsegfau.lt",
        "https://api.piped.projectsegfau.lt"
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Memeriksa apakah sebuah URL adalah URL YouTube.
     */
    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com/watch") ||
               url.contains("youtu.be/") ||
               url.contains("youtube.com/@") ||
               url.contains("youtube.com/live")
    }

    /**
     * Ekstrak Video ID dari berbagai format URL YouTube:
     * - https://www.youtube.com/watch?v=ABC123xyz
     * - https://youtu.be/ABC123xyz
     * - https://www.youtube.com/live/ABC123xyz
     */
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

    /**
     * Resolve YouTube URL → HLS stream URL via Piped API.
     *
     * @param youtubeUrl URL YouTube (youtube.com/watch?v=... atau youtu.be/...)
     * @return HLS m3u8 URL yang bisa diputar ExoPlayer, atau null jika gagal semua instance.
     */
    suspend fun resolveHlsUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrl)
        if (videoId == null) {
            Log.e(TAG, "Gagal ekstrak video ID dari URL: $youtubeUrl")
            return@withContext null
        }

        Log.d(TAG, "Meresolve YouTube video ID: $videoId")

        for (instance in PIPED_INSTANCES) {
            try {
                val apiUrl = "$instance/streams/$videoId"
                Log.d(TAG, "Mencoba Piped instance: $apiUrl")

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().readText()
                    val hlsUrl = parseHlsFromPipedResponse(responseText)

                    if (hlsUrl != null) {
                        Log.d(TAG, "Berhasil resolve via $instance → $hlsUrl")
                        return@withContext hlsUrl
                    } else {
                        Log.w(TAG, "Piped $instance: response OK tapi HLS URL tidak ditemukan")
                    }
                } else {
                    Log.w(TAG, "Piped $instance: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance $instance gagal: ${e.message}")
            }
        }

        Log.e(TAG, "Semua Piped instance gagal untuk video ID: $videoId")
        null
    }

    /**
     * Parse response JSON dari Piped API untuk mendapatkan HLS URL.
     *
     * Response Piped API contoh:
     * {
     *   "hls": "https://manifest.googlevideo.com/api/manifest/hls_playlist/...",
     *   "dash": "...",
     *   "audioStreams": [...],
     *   "videoStreams": [...]
     * }
     *
     * Prioritas: hls field (m3u8) → videoStreams url dengan mimeType video/mp4 tertinggi
     */
    private fun parseHlsFromPipedResponse(json: String): String? {
        return try {
            val obj = JSONObject(json)

            // 1. Cek field "hls" — ini yang paling ideal untuk ExoPlayer
            val hlsUrl = obj.optString("hls", "").takeIf { it.isNotEmpty() && it.startsWith("http") }
            if (hlsUrl != null) return hlsUrl

            // 2. Fallback: ambil video stream terbaik dari videoStreams array
            val videoStreams = obj.optJSONArray("videoStreams")
            if (videoStreams != null && videoStreams.length() > 0) {
                // Cari stream dengan kualitas 480p atau di bawahnya (hemat bandwidth TV stick)
                var bestUrl: String? = null
                var bestQuality = 0

                for (i in 0 until videoStreams.length()) {
                    val stream = videoStreams.getJSONObject(i)
                    val streamUrl = stream.optString("url", "")
                    val quality = stream.optInt("quality", 0) // e.g. 480, 720, 1080
                    val mimeType = stream.optString("mimeType", "")

                    if (streamUrl.startsWith("http") && mimeType.contains("video") && quality <= 480) {
                        if (quality > bestQuality) {
                            bestQuality = quality
                            bestUrl = streamUrl
                        }
                    }
                }
                if (bestUrl != null) return bestUrl
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Piped response JSON: ${e.message}")
            null
        }
    }
}
