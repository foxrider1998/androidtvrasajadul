package com.example.androidtvrasajadul.data

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ChannelRepository {

    private const val TAG = "ChannelRepository"

    /**
     * Dynamic Cloud / GitHub Gist URL for auto-updating IPTV playlist.
     */
    private const val CLOUD_PLAYLIST_URL =
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/id.m3u"

    private const val CACHE_FILE_NAME = "channels_cache.m3u"

    // ─────────────────────────────────────────────────────────────────
    //  5 KATEGORI SALURAN BAWAAN — DIJAMIN SELALU ADA, TIDAK PERNAH
    //  TERTIMPA FILE CACHE (cache hanya dipakai jika tidak ada file M3U
    //  manual di SD card).
    // ─────────────────────────────────────────────────────────────────

    // KATEGORI 1: 🇮🇩 Channel Indonesia (CH 1–19)
    private val kategoriIndonesia = listOf(
        Channel(
            1, "RCTI", listOf(
                "https://edge.rctiplus.com/live/rcti/rcti_720/index.m3u8",
                "https://live.rctiplus.id/live/rcti.m3u8",
                "https://stream.mnc.id/rcti/live.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            2, "SCTV", listOf(
                "https://live.sctv.co.id/live/sctv/playlist.m3u8",
                "https://stream.sctv.co.id/sctv.m3u8",
                "https://cdn.sctv.co.id/live/sctv/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            3, "Indosiar", listOf(
                "https://live.indosiar.com/live/indosiar/playlist.m3u8",
                "https://stream.indosiar.com/indosiar.m3u8",
                "https://cdn.indosiar.com/live/indosiar/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            4, "Trans TV", listOf(
                "https://live.transtv.co.id/transtv/index.m3u8",
                "https://stream.trans.id/transtv/live/index.m3u8",
                "https://cdn.transtv.co.id/hls/transtv.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            5, "Trans 7", listOf(
                "https://live.transtv.co.id/trans7/index.m3u8",
                "https://stream.trans.id/trans7/live/index.m3u8",
                "https://cdn.trans7.co.id/hls/trans7.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            6, "ANTV", listOf(
                "https://live.antv.tv/live/antv/playlist.m3u8",
                "https://stream.antv.tv/antv.m3u8",
                "https://cdn.antv.tv/hls/antv/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            7, "MNCTV", listOf(
                "https://edge.rctiplus.com/live/mnctv/mnctv_720/index.m3u8",
                "https://live.rctiplus.id/live/mnctv.m3u8",
                "https://stream.mnc.id/mnctv/live.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            8, "GTV", listOf(
                "https://edge.rctiplus.com/live/gtv/gtv_720/index.m3u8",
                "https://live.rctiplus.id/live/gtv.m3u8",
                "https://stream.mnc.id/gtv/live.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            9, "iNews", listOf(
                "https://edge.rctiplus.com/live/inews/inews_720/index.m3u8",
                "https://live.rctiplus.id/live/inews.m3u8",
                "https://stream.mnc.id/inews/live.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            10, "Kompas TV", listOf(
                "https://live.kompas.tv/live/kompastv/index.m3u8",
                "https://cdn.kompas.tv/hls/kompastv.m3u8",
                "https://stream.kompas.tv/live/kompastv.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            11, "Metro TV", listOf(
                "https://mcv.metrotvnews.com/hls/metrotv/index.m3u8",
                "https://live.metrotvnews.com/hls/metrotv.m3u8",
                "https://cdn.metrotvnews.com/live/metrotv/master.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            12, "TV One", listOf(
                "https://live.tvone.id/hls/tvone.m3u8",
                "https://stream.tvone.id/live/tvone/index.m3u8",
                "https://cdn.tvone.id/hls/tvone/master.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            13, "TVRI Nasional", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Nasional/hls/Nasional.m3u8",
                "https://live.tvri.go.id/hls/tvri_nasional.m3u8",
                "https://stream.tvri.go.id/nasional/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            14, "RTV", listOf(
                "https://live.rtv.co.id/live/rtv/index.m3u8",
                "https://stream.rtv.co.id/live/rtv.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            15, "BTV", listOf(
                "https://live.beritasatu.com/btv/live.m3u8",
                "https://stream.btv.id/live/btv/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            16, "Nusantara TV", listOf(
                "https://live.nusantaratv.com/hls/ntv.m3u8",
                "https://stream.nusantaratv.com/live/ntv/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            17, "CNN Indonesia", listOf(
                "https://live.cnnindonesia.com/live/cnn/index.m3u8",
                "https://stream.trans.id/cnn/live/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            18, "CNBC Indonesia", listOf(
                "https://live.cnbcindonesia.com/live/cnbc/index.m3u8",
                "https://stream.trans.id/cnbc/live/index.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            19, "TVRI Sport HD", listOf(
                "https://live.tvri.go.id/hls/tvri_sport.m3u8",
                "https://stream.tvri.go.id/sport/index.m3u8",
                "https://cdn.tvri.go.id/hls/tvri_sport/master.m3u8"
            ), "Channel Indonesia"
        )
    )

    // KATEGORI 2: 🌏 Channel Asia & Internasional (CH 20–25)
    private val kategoriAsia = listOf(
        Channel(
            20, "CNA (Channel News Asia)", listOf(
                "https://d2e1asnsl7br7b.cloudfront.net/7782e205e72f43a79a20409a805216e9/index.m3u8",
                "https://cna-hls-en.akamaized.net/hls/live/2012028/cnaen/master.m3u8",
                "https://mediacorp.hls.akamaihd.net/cna/index.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            21, "Al Jazeera English", listOf(
                "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
                "https://live-hls-web-aje.getaj.net/AJE/03.m3u8",
                "https://aljazeera-eng-apple-live.wurl.tv/playlist.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            22, "DW English", listOf(
                "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                "https://dwge-lh.akamaihd.net/i/dwge_1@300412/master.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            23, "France 24 English", listOf(
                "https://static.france24.com/live/F24_EN_LO_HLS/live_web.m3u8",
                "https://f24hls-i.akamaihd.net/hls/live/221193/F24_EN_LO_HLS/master.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            24, "NHK World Japan", listOf(
                "https://nhkworld.akamaized.net/hls/live/2003459/nhkworld/gs_hls.m3u8",
                "https://nhkworld-live.akamaized.net/hls/live/2003459/nhkworld/master.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            25, "ABC News (Australia)", listOf(
                "https://abcnews-live.akamaized.net/hls/live/2023314/abcnews/master.m3u8",
                "https://abcnews.com/live/stream.m3u8"
            ), "Asia & Internasional"
        )
    )

    // KATEGORI 3: 🎨 Channel Anime & Kartun (CH 26–30)
    private val kategoriAnime = listOf(
        Channel(
            26, "Animax Asia", listOf(
                "https://cdn.animaxasia.com/live/animax/stream.m3u8",
                "https://stream.animaxasia.com/live/index.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            27, "Cartoon Network", listOf(
                "https://live.cartoonnetwork.com/hls/cn/stream.m3u8",
                "https://cdn.cartoonnetwork.com/live/cn.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            28, "Pokemon TV", listOf(
                "https://www.pokemon.com/us/pokemon-episodes/live/stream.m3u8",
                "https://cdn.pokemon.com/live/stream.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            29, "Duck TV", listOf(
                "https://live.ducktv.com/hls/ducktv.m3u8",
                "https://stream.ducktv.com/live/index.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            30, "Kidoodle", listOf(
                "https://live.kidoodle.tv/hls/kidoodle.m3u8",
                "https://stream.kidoodle.tv/live/index.m3u8"
            ), "Anime & Kartun"
        )
    )

    // KATEGORI 4: 🕌 Pengajian Gus Baha (YouTube) (CH 31–38)
    // Video ID dari channel Nuansa Ilmu Official & Gus Baha TV
    // Diputar via Piped API → HLS stream bebas iklan, tanpa browser
    private val kategoriGusBaha = listOf(
        Channel(
            31, "Gus Baha – Kitab Hikam", listOf(
                "https://www.youtube.com/watch?v=3zZGJb7Ddqw",  // Kitab Hikam sesi 1
                "https://www.youtube.com/watch?v=9oB6rBOIe6s"   // Kitab Hikam sesi 2
            ), "Pengajian Gus Baha"
        ),
        Channel(
            32, "Gus Baha – Tafsir Al-Quran", listOf(
                "https://www.youtube.com/watch?v=XdB0dHFAa78",  // Tafsir Al-Quran Gus Baha
                "https://www.youtube.com/watch?v=5EjXxZlmyZI"   // Tafsir seri lanjutan
            ), "Pengajian Gus Baha"
        ),
        Channel(
            33, "Gus Baha – Fiqih Keseharian", listOf(
                "https://www.youtube.com/watch?v=IcDDDzFY4YA",  // Fiqih santri
                "https://www.youtube.com/watch?v=rH2M5cVsOgA"   // Fiqih muamalah
            ), "Pengajian Gus Baha"
        ),
        Channel(
            34, "Gus Baha – Pengajian Malam", listOf(
                "https://www.youtube.com/watch?v=0k0PbMgBYkE",  // Pengajian malam
                "https://www.youtube.com/watch?v=GXlKe2_I7vc"   // Pengajian rutin
            ), "Pengajian Gus Baha"
        ),
        Channel(
            35, "Gus Baha – Ulumul Hadits", listOf(
                "https://www.youtube.com/watch?v=gu1gW9a7t3s",  // Hadits seri 1
                "https://www.youtube.com/watch?v=vC2UtTiLuHo"   // Hadits seri 2
            ), "Pengajian Gus Baha"
        ),
        Channel(
            36, "Gus Baha – Tafsir Yasin", listOf(
                "https://www.youtube.com/watch?v=l8e1nVCbZWU",  // Tafsir Yasin
                "https://www.youtube.com/watch?v=cK5wIyTQ5SU"   // Tafsir Yasin lanjutan
            ), "Pengajian Gus Baha"
        ),
        Channel(
            37, "Gus Baha – Santri Gayeng", listOf(
                "https://www.youtube.com/watch?v=dQqp1K0QVIY",  // Santri Gayeng ep 1
                "https://www.youtube.com/watch?v=JKA1eHpVJTE"   // Santri Gayeng ep 2
            ), "Pengajian Gus Baha"
        ),
        Channel(
            38, "Gus Baha – Nasihat & Hikmah", listOf(
                "https://www.youtube.com/watch?v=CrI9_JQUgRk",  // Nasihat pilihan
                "https://www.youtube.com/watch?v=ekCJQZF6FnQ"   // Hikmah santri
            ), "Pengajian Gus Baha"
        )
    )

    // KATEGORI 5: 📺 Channel Daerah & Religi (CH 39–43)
    private val kategoriDaerahReligi = listOf(
        Channel(
            39, "Jawa Pos TV", listOf(
                "https://live.jawapostv.com/hls/stream.m3u8",
                "https://stream.jawapostv.com/live.m3u8"
            ), "Daerah & Religi"
        ),
        Channel(
            40, "Bali TV", listOf(
                "https://live.balitv.tv/hls/balitv.m3u8",
                "https://stream.balitv.tv/live/index.m3u8"
            ), "Daerah & Religi"
        ),
        Channel(
            41, "Rodja TV", listOf(
                "https://live.radiorodja.com/hls/rodjatv.m3u8",
                "https://stream.rodja.tv/live/index.m3u8"
            ), "Daerah & Religi"
        ),
        Channel(
            42, "TV Mu", listOf(
                "https://live.tvmu.tv/hls/tvmu.m3u8",
                "https://stream.tvmu.tv/live/index.m3u8"
            ), "Daerah & Religi"
        ),
        Channel(
            43, "Surau TV", listOf(
                "https://live.surautv.org/hls/surautv.m3u8",
                "https://stream.surautv.org/live/index.m3u8"
            ), "Daerah & Religi"
        )
    )

    /**
     * Daftar kategori terurut (untuk navigasi KIRI/KANAN).
     * Urutan ini TIDAK BERUBAH dan dijamin selalu tersedia.
     */
    val categories: List<String> = listOf(
        "Channel Indonesia",     // 0: CH 1–19
        "Asia & Internasional",  // 1: CH 20–25
        "Anime & Kartun",        // 2: CH 26–30
        "Pengajian Gus Baha",    // 3: CH 31–38
        "Daerah & Religi"        // 4: CH 39–43
    )

    /**
     * Emoji prefix per kategori untuk banner yang menarik.
     */
    val categoryEmoji: Map<String, String> = mapOf(
        "Channel Indonesia"    to "🇮🇩",
        "Asia & Internasional" to "🌏",
        "Anime & Kartun"       to "🎨",
        "Pengajian Gus Baha"   to "🕌",
        "Daerah & Religi"      to "📺"
    )

    /**
     * Seluruh daftar saluran bawaan yang dijamin tersedia,
     * disusun berurutan sesuai 5 kategori.
     */
    private val defaultChannels: List<Channel> =
        kategoriIndonesia +
        kategoriAsia +
        kategoriAnime +
        kategoriGusBaha +
        kategoriDaerahReligi

    /**
     * Mendapatkan saluran pertama dari suatu kategori.
     */
    fun getFirstChannelOfCategory(categoryName: String): Channel? {
        return defaultChannels.firstOrNull { it.category == categoryName }
    }

    /**
     * Mendapatkan indeks saluran pertama dari suatu kategori dalam daftar channels aktif.
     */
    fun getFirstIndexOfCategory(channels: List<Channel>, categoryName: String): Int {
        return channels.indexOfFirst { it.category == categoryName }
    }

    /**
     * Loads channels from local sources. Priority:
     * 1. `/sdcard/channels.m3u` (Manual custom file → SELALU gunakan defaultChannels sebagai basis)
     * 2. Built-in default channels (TIDAK dapat ditimpa oleh cache otomatis)
     *
     * CATATAN: Cache cloud m3u TIDAK dipakai agar 5 kategori bawaan tidak pernah hilang.
     */
    fun loadChannels(context: Context): List<Channel> {
        // Priority 1: External SD card file — hanya jika pengguna taruh file manual
        val externalFiles = listOf(
            File("/sdcard/channels.m3u"),
            File(Environment.getExternalStorageDirectory(), "channels.m3u"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "channels.m3u")
        )
        val sdCardFile = externalFiles.firstOrNull { it.exists() && it.canRead() }
        if (sdCardFile != null) {
            Log.d(TAG, "Loading channels from SD card M3U: ${sdCardFile.absolutePath}")
            val parsed = parseM3uFile(sdCardFile, isCustom = true)
            if (parsed.isNotEmpty()) {
                // Gabungkan: saluran custom di depan, default di belakang (tidak tertimpa)
                return parsed + defaultChannels.filter { def ->
                    parsed.none { it.number == def.number }
                }
            }
        }

        // Priority 2: Built-in 5 kategori — SELALU tersedia
        Log.d(TAG, "Loading built-in 5-category channel list (${defaultChannels.size} channels).")
        return defaultChannels
    }

    /**
     * Background task to download the latest playlist from cloud/GitHub repository.
     * Saves to `context.filesDir/channels_cache.m3u` (hanya sebagai referensi, tidak dipakai untuk load).
     */
    suspend fun updatePlaylistFromCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching playlist update from cloud: $CLOUD_PLAYLIST_URL")
            val url = URL(CLOUD_PLAYLIST_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                FileOutputStream(cacheFile).use { output ->
                    inputStream.copyTo(output)
                }
                Log.d(TAG, "Successfully downloaded and cached playlist to ${cacheFile.absolutePath}")
                return@withContext true
            } else {
                Log.w(TAG, "Cloud playlist download failed with response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating playlist from cloud in background: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Parses an M3U file into a list of Channels, combining duplicate channel names into multi-link fallback entries.
     */
    private fun parseM3uFile(file: File, isCustom: Boolean): List<Channel> {
        val channelMap = mutableMapOf<String, MutableList<String>>()
        val channelOrder = mutableListOf<String>()

        try {
            val lines = file.readLines()
            var currentTitle: String? = null

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    val commaIndex = trimmed.lastIndexOf(',')
                    currentTitle = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Channel ${channelOrder.size + 1}"
                    }
                } else if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.startsWith("rtmp://", ignoreCase = true)
                ) {
                    val title = currentTitle ?: "Channel ${channelOrder.size + 1}"
                    if (!channelMap.containsKey(title)) {
                        channelMap[title] = mutableListOf()
                        channelOrder.add(title)
                    }
                    channelMap[title]?.add(trimmed)
                    currentTitle = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U file ${file.absolutePath}: ${e.message}")
        }

        return channelOrder.mapIndexed { index, name ->
            val urls = channelMap[name] ?: emptyList()
            Channel(
                number = index + 1,
                name = name,
                urls = urls,
                category = "Custom",
                isCustom = isCustom
            )
        }
    }
}
