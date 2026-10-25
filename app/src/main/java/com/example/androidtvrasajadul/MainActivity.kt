package com.example.androidtvrasajadul

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.androidtvrasajadul.data.Channel
import com.example.androidtvrasajadul.data.ChannelRepository
import com.example.androidtvrasajadul.databinding.ActivityMainBinding
import com.example.androidtvrasajadul.utils.PreferenceManager
import com.example.androidtvrasajadul.utils.YouTubeResolver
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Android TV Launcher designed specifically for elderly parents ("rasa TV jadul").
 *
 * Features & Improvements:
 * 1. Gambar Semut TV Dinamis (CRT TV Static Noise) setiap kali ganti channel/kategori.
 *    - Muncul seketika saat tombol UP/DOWN/angka/KIRI/KANAN ditekan.
 *    - Hilang otomatis begitu stream baru siap (STATE_READY).
 *    - Tetap menyala + tulisan "TIDAK ADA SINYAL" jika semua link gagal (3x percobaan).
 * 2. 5 Kategori Saluran Bawaan dijamin SELALU tersedia (tidak tertimpa cache):
 *    - KIRI/KANAN  → Ganti KATEGORI (berpindah ke saluran pertama kategori tsb.)
 *    - ATAS/BAWAH  → Ganti CHANNEL dalam kategori aktif (atau lintas kategori)
 *    - Tombol Angka → Langsung ke nomor channel
 * 3. Multi-link fallback (Primary → Backup1 → Backup2...) saat stream error.
 * 4. Header & User-Agent spoofing untuk bypass CDN block.
 * 5. Background cloud playlist update tanpa menghambat startup.
 * 6. SurfaceView/ExoPlayer tuned untuk Android TV stick 32-bit.
 * 7. Key interception: BACK tidak pernah keluar ke layar hitam.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val OSD_DURATION_MS = 3000L
        private const val DIGIT_TIMEOUT_MS = 1500L
        private const val CATEGORY_BANNER_MS = 2500L
        private const val PERMISSION_REQUEST_CODE = 1001

        /** Maksimal percobaan fallback URL sebelum tampil "TIDAK ADA SINYAL" */
        private const val MAX_RETRY_ATTEMPTS = 3

        // Modern Browser User-Agent string to prevent CDN HTTP 403/401 blocking
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager

    private var player: ExoPlayer? = null
    private var channels: List<Channel> = emptyList()
    private var currentChannelIndex: Int = 0
    private var currentUrlIndex: Int = 0

    /** Indeks kategori aktif dalam ChannelRepository.categories (0–4) */
    private var currentCategoryIndex: Int = 0

    /**
     * Jumlah percobaan fallback yang telah dilakukan untuk channel saat ini.
     * Jika mencapai MAX_RETRY_ATTEMPTS, tampilkan "TIDAK ADA SINYAL" dengan gambar semut tetap.
     */
    private var retryCount: Int = 0

    /**
     * Apakah gambar semut sedang aktif karena ganti channel (bukan karena NO SIGNAL permanen).
     * true  = sedang transisi channel, gambar semut akan hilang saat STATE_READY
     * false = gambar semut TIDAK ditampilkan, atau sudah permanen (NO SIGNAL)
     */
    private var isChannelSwitching: Boolean = false

    // OSD Handler & Runnable
    private val osdHandler = Handler(Looper.getMainLooper())
    private val osdRunnable = Runnable { hideOsd() }

    // Category Banner Handler
    private val categoryHandler = Handler(Looper.getMainLooper())
    private val categoryRunnable = Runnable { hideCategoryBanner() }

    // Remote Number Input Buffering
    private val digitBuffer = StringBuilder()
    private val digitHandler = Handler(Looper.getMainLooper())
    private val digitRunnable = Runnable { commitDigitInput() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        loadChannelsAndPreparePlayer()

        // Background auto-update cloud playlist from GitHub/Cloud repo
        updateCloudPlaylistInBackground()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not launch MANAGE_APP_ALL_FILES_ACCESS_PERMISSION intent", e)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun loadChannelsAndPreparePlayer() {
        channels = ChannelRepository.loadChannels(this)

        if (channels.isEmpty()) {
            Log.e(TAG, "No channels found!")
            return
        }

        // Restore last watched channel index from SharedPreferences
        val lastSavedIndex = preferenceManager.getLastChannelIndex()
        currentChannelIndex = if (lastSavedIndex in channels.indices) {
            lastSavedIndex
        } else {
            0
        }

        // Restore current category index based on the restored channel
        val restoredCategory = channels.getOrNull(currentChannelIndex)?.category
        val catIdx = ChannelRepository.categories.indexOf(restoredCategory)
        currentCategoryIndex = if (catIdx >= 0) catIdx else 0

        setupExoPlayer()
        playChannel(currentChannelIndex, showStatic = false)
    }

    /**
     * Downloads latest playlist from cloud in a background thread without stalling app boot.
     */
    private fun updateCloudPlaylistInBackground() {
        lifecycleScope.launch {
            val updated = ChannelRepository.updatePlaylistFromCloud(this@MainActivity)
            if (updated) {
                Log.d(TAG, "Cloud playlist updated successfully (cache only, default channels preserved).")
            }
        }
    }

    /**
     * Sets up ExoPlayer with header/user-agent spoofing, track selection locked to 480p max,
     * and buffer cache locked to min 2000ms.
     */
    private fun setupExoPlayer() {
        // HTTP Data Source Factory with User-Agent & Referer Headers
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.google.com/",
                    "Origin" to "https://www.google.com",
                    "Accept" to "*/*"
                )
            )

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // TrackSelector: Lock max streaming resolution to 480p (max 720p)
        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(854, 480)
                .setPreferredVideoMimeType("video/avc")
                .build()
        }

        // LoadControl: Set min buffer cache to 2000ms
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,  // minBufferMs (minimal 2000ms)
                10000, // maxBufferMs
                1500,  // bufferForPlaybackMs
                2000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        binding.playerView.player = player

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // Gambar semut sudah aktif sejak tombol ditekan, cukup update status
                        binding.statusContainer.visibility = View.VISIBLE
                        binding.tvStatusMessage.text = getString(R.string.loading_stream)
                    }
                    Player.STATE_READY -> {
                        // Stream siap → hentikan gambar semut & sembunyikan status
                        if (isChannelSwitching) {
                            hideStaticNoise()
                            isChannelSwitching = false
                        }
                        binding.statusContainer.visibility = View.GONE
                        retryCount = 0
                    }
                    Player.STATE_ENDED -> {
                        player?.seekTo(0)
                        player?.prepare()
                    }
                    Player.STATE_IDLE -> {
                        // Idle state
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error on url index $currentUrlIndex: ${error.message}", error)
                handlePlaybackError()
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────
    //  GAMBAR SEMUT TV — SHOW / HIDE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tampilkan gambar semut TV dinamis (CRT TV Static Noise).
     * Dipanggil SETIAP KALI tombol ganti channel / ganti kategori ditekan.
     */
    private fun showStaticNoise() {
        binding.tvStaticNoiseView.visibility = View.VISIBLE
        binding.tvStaticNoiseView.startNoise()
        isChannelSwitching = true
    }

    /**
     * Sembunyikan gambar semut TV (dipanggil saat STATE_READY atau secara manual).
     */
    private fun hideStaticNoise() {
        binding.tvStaticNoiseView.stopNoise()
        binding.tvStaticNoiseView.visibility = View.GONE
    }

    /**
     * Tampilkan gambar semut TV PERMANEN + tulisan "TIDAK ADA SINYAL"
     * saat semua link gagal setelah MAX_RETRY_ATTEMPTS.
     */
    private fun showNoSignal() {
        // Gambar semut tetap menyala
        binding.tvStaticNoiseView.visibility = View.VISIBLE
        binding.tvStaticNoiseView.startNoise()
        isChannelSwitching = false // permanen, tidak auto-hilang

        // Tampilkan overlay "TIDAK ADA SINYAL"
        binding.statusContainer.visibility = View.VISIBLE
        binding.tvStatusMessage.text = getString(R.string.no_signal_title)
        binding.progressBar.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────────
    //  CHANNEL PLAYBACK
    // ─────────────────────────────────────────────────────────────────

    /**
     * Plays channel at the specified list index starting from primary URL (index 0).
     * @param showStatic true = tampilkan gambar semut dulu (saat user tekan tombol)
     */
    private fun playChannel(index: Int, showStatic: Boolean = true) {
        if (channels.isEmpty() || index !in channels.indices) return

        currentChannelIndex = index
        currentUrlIndex = 0
        retryCount = 0

        val channel = channels[currentChannelIndex]
        Log.d(TAG, "Playing channel ${channel.number}: ${channel.name} (Primary URL: ${channel.primaryUrl})")

        preferenceManager.saveLastChannel(currentChannelIndex, channel.number)

        // Perbarui indeks kategori agar sinkron
        val catIdx = ChannelRepository.categories.indexOf(channel.category)
        if (catIdx >= 0) currentCategoryIndex = catIdx

        if (showStatic) {
            showStaticNoise()
            // Sembunyikan status lama, reset progress bar
            binding.progressBar.visibility = View.VISIBLE
            binding.statusContainer.visibility = View.GONE
        }

        startPlaybackForCurrentUrl()
        showOsd(OSD_DURATION_MS)
    }

    /**
     * Starts playback for `currentUrlIndex` of `channels[currentChannelIndex]`.
     *
     * Jika URL adalah YouTube (youtube.com/watch?v=...), resolve dulu ke HLS URL
     * via Piped API secara async, lalu baru diputar ExoPlayer — bebas iklan!
     */
    private fun startPlaybackForCurrentUrl() {
        val channel = channels.getOrNull(currentChannelIndex) ?: return
        val url = channel.urls.getOrNull(currentUrlIndex) ?: return

        if (YouTubeResolver.isYouTubeUrl(url)) {
            // URL YouTube → resolve via Piped API dulu
            resolveAndPlayYouTube(url)
        } else {
            // URL langsung (HLS/RTMP) → langsung play
            playResolvedUrl(url)
        }
    }

    /**
     * Resolve YouTube URL ke HLS stream via Piped API, lalu putar.
     * Gambar semut tetap menyala selama proses resolve berlangsung.
     */
    private fun resolveAndPlayYouTube(youtubeUrl: String) {
        // Update status: menghubungkan ke YouTube via Piped
        binding.statusContainer.visibility = View.VISIBLE
        binding.tvStatusMessage.text = "⏳ Memuat YouTube (bebas iklan)..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val hlsUrl = YouTubeResolver.resolveHlsUrl(youtubeUrl)

            if (hlsUrl != null) {
                Log.d(TAG, "YouTube resolved → $hlsUrl")
                playResolvedUrl(hlsUrl)
            } else {
                Log.e(TAG, "Gagal resolve YouTube URL: $youtubeUrl")
                // Treat seperti playback error → coba URL fallback berikutnya
                handlePlaybackError()
            }
        }
    }

    /**
     * Memutar URL yang sudah siap (HLS/RTMP) langsung ke ExoPlayer.
     */
    private fun playResolvedUrl(url: String) {
        player?.let { p ->
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            p.setMediaItem(mediaItem)
            p.prepare()
            p.playWhenReady = true
        }
    }

    /**
     * Multi-Link Fallback Mechanism:
     * Jika URL saat ini gagal → coba URL cadangan berikutnya.
     * Jika semua URL (hingga MAX_RETRY_ATTEMPTS) gagal → tampilkan "TIDAK ADA SINYAL".
     */
    private fun handlePlaybackError() {
        if (channels.isEmpty() || currentChannelIndex !in channels.indices) return

        val channel = channels[currentChannelIndex]
        val totalUrls = channel.urls.size
        retryCount++

        if (currentUrlIndex + 1 < totalUrls && retryCount <= MAX_RETRY_ATTEMPTS) {
            currentUrlIndex++
            Log.w(TAG, "Link gagal untuk ${channel.name}. Mencoba backup URL ${currentUrlIndex + 1}/$totalUrls...")

            val backupMsg = getString(R.string.retrying_stream, retryCount, MAX_RETRY_ATTEMPTS)
            binding.statusContainer.visibility = View.VISIBLE
            binding.tvStatusMessage.text = backupMsg
            binding.progressBar.visibility = View.VISIBLE

            startPlaybackForCurrentUrl()
        } else {
            Log.e(TAG, "Semua $totalUrls link gagal untuk channel ${channel.name} setelah $retryCount percobaan.")
            showNoSignal()
            showOsdMessage(getString(R.string.no_signal_title))
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGASI CHANNEL (ATAS / BAWAH)
    // ─────────────────────────────────────────────────────────────────

    private fun nextChannel() {
        if (channels.isEmpty()) return
        val nextIndex = (currentChannelIndex + 1) % channels.size
        playChannel(nextIndex, showStatic = true)
    }

    private fun previousChannel() {
        if (channels.isEmpty()) return
        val prevIndex = if (currentChannelIndex - 1 < 0) {
            channels.size - 1
        } else {
            currentChannelIndex - 1
        }
        playChannel(prevIndex, showStatic = true)
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGASI KATEGORI (KIRI / KANAN)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Beralih ke kategori BERIKUTNYA (tombol KANAN).
     * Menampilkan gambar semut TV + banner kategori emas, lalu langsung putar
     * saluran pertama dari kategori tersebut.
     */
    private fun nextCategory() {
        currentCategoryIndex = (currentCategoryIndex + 1) % ChannelRepository.categories.size
        switchToCurrentCategory()
    }

    /**
     * Beralih ke kategori SEBELUMNYA (tombol KIRI).
     */
    private fun previousCategory() {
        currentCategoryIndex = if (currentCategoryIndex - 1 < 0) {
            ChannelRepository.categories.size - 1
        } else {
            currentCategoryIndex - 1
        }
        switchToCurrentCategory()
    }

    /**
     * Tampilkan gambar semut + banner emas, lalu putar channel pertama kategori.
     */
    private fun switchToCurrentCategory() {
        val categoryName = ChannelRepository.categories[currentCategoryIndex]
        val emoji = ChannelRepository.categoryEmoji[categoryName] ?: "📺"
        val totalCategories = ChannelRepository.categories.size
        val categoryNum = currentCategoryIndex + 1

        // 1. Tampilkan gambar semut TV
        showStaticNoise()
        binding.progressBar.visibility = View.VISIBLE
        binding.statusContainer.visibility = View.GONE

        // 2. Tampilkan banner kategori emas
        val bannerText = "KATEGORI [$categoryNum/$totalCategories]: $emoji $categoryName"
        binding.tvCategoryBanner.text = bannerText
        binding.categoryBannerContainer.visibility = View.VISIBLE

        categoryHandler.removeCallbacks(categoryRunnable)
        categoryHandler.postDelayed(categoryRunnable, CATEGORY_BANNER_MS)

        // 3. Cari dan putar saluran pertama dari kategori ini
        val targetIndex = ChannelRepository.getFirstIndexOfCategory(channels, categoryName)
        if (targetIndex >= 0) {
            // playChannel dengan showStatic=false karena kita sudah panggil showStaticNoise() manual
            currentChannelIndex = targetIndex
            currentUrlIndex = 0
            retryCount = 0
            preferenceManager.saveLastChannel(currentChannelIndex, channels[currentChannelIndex].number)
            startPlaybackForCurrentUrl()
            showOsd(OSD_DURATION_MS)
        } else {
            // Kategori tidak ada channel yang cocok (tidak seharusnya terjadi)
            showNoSignal()
        }
    }

    private fun hideCategoryBanner() {
        binding.categoryBannerContainer.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────────
    //  OSD (ON SCREEN DISPLAY)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Displays High-Contrast OSD dengan nomor & nama channel + kategori.
     */
    private fun showOsd(durationMs: Long = OSD_DURATION_MS) {
        if (channels.isEmpty() || currentChannelIndex !in channels.indices) return

        val currentChannel = channels[currentChannelIndex]
        val formattedNumber = String.format(Locale.getDefault(), "CH %02d", currentChannel.number)

        binding.tvOsdChannelNumber.text = formattedNumber
        binding.tvOsdChannelName.text = "${currentChannel.name} • [${currentChannel.category}]"

        val qualityText = if (currentChannel.isCustom) {
            "M3U • SD 480p"
        } else {
            getString(R.string.osd_quality_label)
        }
        binding.tvOsdQuality.text = qualityText

        binding.osdContainer.visibility = View.VISIBLE

        osdHandler.removeCallbacks(osdRunnable)
        osdHandler.postDelayed(osdRunnable, durationMs)
    }

    private fun hideOsd() {
        osdHandler.removeCallbacks(osdRunnable)
        binding.osdContainer.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────────
    //  INPUT ANGKA REMOTE
    // ─────────────────────────────────────────────────────────────────

    private fun handleNumberInput(digit: Int) {
        if (digitBuffer.length >= 3) {
            digitBuffer.clear()
        }
        digitBuffer.append(digit)

        binding.tvDigitInput.text = getString(R.string.digit_input_prompt, digitBuffer.toString())
        binding.digitContainer.visibility = View.VISIBLE

        // Tampilkan gambar semut seketika saat pengguna mulai mengetik angka
        if (!isChannelSwitching) {
            showStaticNoise()
        }

        digitHandler.removeCallbacks(digitRunnable)
        digitHandler.postDelayed(digitRunnable, DIGIT_TIMEOUT_MS)
    }

    private fun commitDigitInput() {
        val targetNumStr = digitBuffer.toString()
        digitBuffer.clear()
        binding.digitContainer.visibility = View.GONE

        val targetNum = targetNumStr.toIntOrNull() ?: run {
            hideStaticNoise()
            return
        }
        val foundIndex = channels.indexOfFirst { it.number == targetNum }

        if (foundIndex != -1) {
            // Gambar semut sudah tampil sejak user tekan angka pertama
            currentChannelIndex = foundIndex
            currentUrlIndex = 0
            retryCount = 0
            val channel = channels[foundIndex]
            preferenceManager.saveLastChannel(foundIndex, channel.number)
            val catIdx = ChannelRepository.categories.indexOf(channel.category)
            if (catIdx >= 0) currentCategoryIndex = catIdx
            binding.progressBar.visibility = View.VISIBLE
            binding.statusContainer.visibility = View.GONE
            startPlaybackForCurrentUrl()
            showOsd(OSD_DURATION_MS)
        } else {
            hideStaticNoise()
            showOsdMessage(getString(R.string.channel_not_found, targetNumStr))
        }
    }

    private fun showOsdMessage(message: String) {
        binding.tvOsdChannelNumber.text = "INFO"
        binding.tvOsdChannelName.text = message
        binding.osdContainer.visibility = View.VISIBLE

        osdHandler.removeCallbacks(osdRunnable)
        osdHandler.postDelayed(osdRunnable, OSD_DURATION_MS)
    }

    // ─────────────────────────────────────────────────────────────────
    //  REMOTE CONTROL KEY INTERCEPTION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Remote Control Navigation Interception for Elderly UX.
     *
     * Mapping:
     *  ATAS / CH+        → Channel berikutnya (dalam semua channel berurutan)
     *  BAWAH / CH-       → Channel sebelumnya
     *  KANAN             → Kategori BERIKUTNYA (langsung ke channel pertama kategori)
     *  KIRI              → Kategori SEBELUMNYA (langsung ke channel pertama kategori)
     *  ENTER / OK        → Tampilkan OSD
     *  0–9 / Numpad 0–9 → Ketik nomor channel (dengan gambar semut langsung)
     *  BACK              → Tutup OSD/digit overlay (TIDAK keluar dari app)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                nextChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                previousChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                nextCategory()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                previousCategory()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                showOsd(OSD_DURATION_MS)
                return true
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_0
                handleNumberInput(digit)
                return true
            }
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                handleNumberInput(digit)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (binding.osdContainer.visibility == View.VISIBLE ||
                    binding.digitContainer.visibility == View.VISIBLE ||
                    binding.categoryBannerContainer.visibility == View.VISIBLE) {
                    hideOsd()
                    binding.digitContainer.visibility = View.GONE
                    binding.categoryBannerContainer.visibility = View.GONE
                    digitBuffer.clear()
                    // Jika gambar semut masih transitioning, hentikan
                    if (isChannelSwitching) {
                        hideStaticNoise()
                    }
                }
                return true // Always intercept BACK button to prevent exiting launcher!
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // ─────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        if (player == null) {
            setupExoPlayer()
            playChannel(currentChannelIndex, showStatic = false)
        } else {
            player?.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        osdHandler.removeCallbacks(osdRunnable)
        digitHandler.removeCallbacks(digitRunnable)
        categoryHandler.removeCallbacks(categoryRunnable)
        player?.release()
        player = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadChannelsAndPreparePlayer()
            }
        }
    }
}
