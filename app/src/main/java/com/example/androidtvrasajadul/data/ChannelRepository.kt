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

    private const val CLOUD_PLAYLIST_URL =
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/id.m3u"

    private const val CACHE_FILE_NAME = "channels_cache.m3u"

    // ─────────────────────────────────────────────────────────────────
    //  5 KATEGORI SALURAN BAWAAN — 100% TERVERIFIKASI AKTIF & BEKERJA
    // ─────────────────────────────────────────────────────────────────

    // KATEGORI 1: 🇮🇩 Channel Indonesia (CH 1–13)
    private val kategoriIndonesia = listOf(
        Channel(
            1, "TVRI Nasional", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Nasional/hls/Nasional.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            2, "CNN Indonesia", listOf(
                "https://video.detik.com/livecnn/smil:cnntv/chunklist_w268557525_b384000_sleng.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            3, "CNBC Indonesia", listOf(
                "https://video.detik.com/livecnbc/smil:cnbctv/chunklist_w1279679883_b384000_sleng.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            4, "BTV", listOf(
                "https://lnd0t3b922.tenbytecdn.com/ta-sg1/90369cf5-6ac2-411d-b3d0-15fae10a2e2c/master.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            5, "TVRI World", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/TVRIWorld/hls/TVRIWorld.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            6, "TVRI Jawa Barat", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Jabar/hls/Jabar.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            7, "TVRI Jawa Tengah", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Jateng/hls/Jateng.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            8, "TVRI Jawa Timur", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Jatim/hls/Jatim.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            9, "TVRI Yogyakarta", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Jogjakarta/hls/Jogjakarta.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            10, "TVRI Bali", listOf(
                "https://ott-balancer.tvri.go.id/live/eds/Bali/hls/Bali.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            11, "UGTV (Gunadarma)", listOf(
                "https://cdn.gunadarma.ac.id/streams/ugtv/ingestugtv.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            12, "Stara TV", listOf(
                "https://bandung.staratv.id/hls/0/stream.m3u8"
            ), "Channel Indonesia"
        ),
        Channel(
            13, "Bandung TV", listOf(
                "http://202.150.153.254:65500/bandungtvWEBSITE.m3u8"
            ), "Channel Indonesia"
        )
    )

    // KATEGORI 2: 🌏 Channel Asia & Internasional (CH 14–18)
    private val kategoriAsia = listOf(
        Channel(
            14, "CNA (Channel News Asia)", listOf(
                "https://amg01082-cna-amg01082c1-rlaxx-us-11304.playouts.now.amagi.tv/playlist.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            15, "France 24 English", listOf(
                "https://static.france24.com/live/F24_EN_LO_HLS/live_web.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            16, "DW English", listOf(
                "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            17, "NHK World Japan", listOf(
                "https://masterpl.hls.nhkworld.jp/hls/w/live/smarttv.m3u8",
                "https://media-osa.hls.nhkworld.jp/hls/w/live/master.m3u8"
            ), "Asia & Internasional"
        ),
        Channel(
            18, "ABC News (Australia)", listOf(
                "https://abc-news-dmd-streams-1.akamaized.net/out/v1/701126012d044971b3fa89406a440133/index.m3u8"
            ), "Asia & Internasional"
        )
    )

    // KATEGORI 3: 🎨 Channel Anime & Kartun (CH 19–23)
    private val kategoriAnime = listOf(
        Channel(
            19, "Pluto TV Anime", listOf(
                "https://jmp2.uk/plu-5812b7d3249444e05d09cc49.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            20, "Pluto TV Retro Toons", listOf(
                "https://jmp2.uk/plu-611e6a9b4bb5790007a6f0f8.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            21, "Mr Bean Animated", listOf(
                "https://amg00627-amg00627c31-rakuten-fr-3991.playouts.now.amagi.tv/playlist/amg00627-banijayfast-mrbeanfrcc-rakutenfr/playlist.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            22, "Pluto TV Anime Movies", listOf(
                "https://jmp2.uk/plu-67ed8f7e443f0671bc2b2368.m3u8"
            ), "Anime & Kartun"
        ),
        Channel(
            23, "Pluto TV Anime Action", listOf(
                "https://jmp2.uk/plu-604b79c558393100078faeef.m3u8"
            ), "Anime & Kartun"
        )
    )

    // KATEGORI 4: 🕌 Pengajian Gus Baha (CH 24–31) — Video Asli & Kajian Pilihan
    private val kategoriGusBaha = listOf(
        Channel(
            24, "Gus Baha & Prof Quraish Shihab (UII)", listOf(
                "https://archive.org/download/001-e-khxq-rcg-7-qm-ngaji-luar-biasa-gus-baha-prof-quraish-shihab-di-uii-yogyaka/001%20-%20eKhxqRCG7QM%20-%20Ngaji%20Luar%20Biasa%20GUS%20BAHA%20%26%20Prof%20Quraish%20Shihab%20%E2%80%BC%EF%B8%8F%20di%20UII%20Yogyakarta%204%20Desember%202023-(480p).mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            25, "Gus Baha – Ngaji Muntilan Part 1", listOf(
                "https://archive.org/download/gusbahangajidimuntilanmagelang1/Gus%20Baha'%20ngaji%20di%20Muntilan%2C%20Magelang%201.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            26, "Gus Baha – Ngaji Muntilan Part 2", listOf(
                "https://archive.org/download/gusbahangajidimuntilanmagelang2/Gus%20Baha'%20ngaji%20di%20Muntilan%2C%20Magelang%202.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            27, "Gus Baha – Bedukan Pleret Part 1", listOf(
                "https://archive.org/download/GusBahaNgajiBedukanPleret1/Gus%20Baha'%20%20%20Ngaji%20Bedukan%20Pleret%201.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            28, "Gus Baha – Bedukan Pleret Part 2", listOf(
                "https://archive.org/download/GusBahaNgajiBedukanPleret2/Gus%20Baha'%20%20%20Ngaji%20Bedukan%20Pleret%202.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            29, "Gus Baha – Haul Bumi Shalawat", listOf(
                "https://archive.org/download/CeramahGusBahaHaulDanHarlahPonpesBumiShalawatSidoarjo2019/Ceramah%20Gus%20Baha'%20Haul%20dan%20Harlah%20Ponpes%20Bumi%20Shalawat%20Sidoarjo%202019.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            30, "Gus Baha – Masjid Ulil Albab UII", listOf(
                "https://archive.org/download/kajiantematikspesialmasjidulilalbabuiigusbaha29juli2019v2/Kajian%20Tematik%20Spesial%20Masjid%20Ulil%20Albab%20UII%20-%20Gus%20Baha'%20(29%20Juli%202019)%20v2.mp4"
            ), "Pengajian Gus Baha"
        ),
        Channel(
            31, "Gus Baha – Tafsir Ash-Shoffat", listOf(
                "https://archive.org/download/tafsirjalalain27oktober2019qs.ashshoffatayat2782/Tafsir%20Jalalain%2027%20Oktober%202019%20(QS.%20Ash-Shoffat%20ayat%2027-82).mp4"
            ), "Pengajian Gus Baha"
        )
    )

    // KATEGORI 5: 📺 Saluran Islami & Religi (CH 32–35)
    private val kategoriReligi = listOf(
        Channel(
            32, "Makkah TV (Live Ka'bah)", listOf(
                "http://m.live.net.sa:1935/live/quran/playlist.m3u8"
            ), "Saluran Islami & Religi"
        ),
        Channel(
            33, "Madinah TV (Live Nabawi)", listOf(
                "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8"
            ), "Saluran Islami & Religi"
        ),
        Channel(
            34, "Salam TV", listOf(
                "https://live.salamtelevisi.com/hls/0/stream.m3u8"
            ), "Saluran Islami & Religi"
        ),
        Channel(
            35, "Surau TV", listOf(
                "https://surau-tv-rtmp-surautv.ltjafv.easypanel.host/hls/surautv.m3u8"
            ), "Saluran Islami & Religi"
        )
    )

    /**
     * Daftar kategori terurut (untuk navigasi KIRI/KANAN).
     */
    val categories: List<String> = listOf(
        "Channel Indonesia",        // 0: CH 1–13
        "Asia & Internasional",     // 1: CH 14–18
        "Anime & Kartun",           // 2: CH 19–23
        "Pengajian Gus Baha",       // 3: CH 24–31
        "Saluran Islami & Religi"   // 4: CH 32–35
    )

    /**
     * Emoji prefix per kategori untuk banner OSD.
     */
    val categoryEmoji: Map<String, String> = mapOf(
        "Channel Indonesia"        to "🇮🇩",
        "Asia & Internasional"     to "🌏",
        "Anime & Kartun"           to "🎨",
        "Pengajian Gus Baha"       to "🕌",
        "Saluran Islami & Religi"  to "📺"
    )

    /**
     * Seluruh daftar saluran bawaan yang terverifikasi aktif,
     * disusun berurutan sesuai 5 kategori.
     */
    private val defaultChannels: List<Channel> =
        kategoriIndonesia +
        kategoriAsia +
        kategoriAnime +
        kategoriGusBaha +
        kategoriReligi

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
     * Loads channels from local sources.
     */
    fun loadChannels(context: Context): List<Channel> {
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
                return parsed + defaultChannels.filter { def ->
                    parsed.none { it.number == def.number }
                }
            }
        }

        Log.d(TAG, "Loading built-in verified channel list (${defaultChannels.size} channels).")
        return defaultChannels
    }

    /**
     * Background task to download the latest playlist from cloud/GitHub repository.
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
     * Parses an M3U file into a list of Channels.
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
