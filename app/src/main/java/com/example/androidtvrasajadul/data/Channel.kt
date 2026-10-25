package com.example.androidtvrasajadul.data

/**
 * Data model representing a TV Channel with multi-link fallback support.
 *
 * @param number Channel number displayed on OSD (1-20 National, 21-30 International, 31+ Regional/Religi)
 * @param name Channel display name (e.g., "RCTI", "Trans TV")
 * @param urls List of streaming URLs (Primary, Backup 1, Backup 2...)
 * @param category Channel category ("Nasional", "Berita & Luar Negeri", "Daerah & Religi")
 * @param isCustom True if loaded from external or custom M3U playlist
 */
data class Channel(
    val number: Int,
    val name: String,
    val urls: List<String>,
    val category: String = "Nasional",
    val isCustom: Boolean = false
) {
    // Secondary constructor for single URL convenience
    constructor(
        number: Int,
        name: String,
        url: String,
        category: String = "Nasional",
        isCustom: Boolean = false
    ) : this(number, name, listOf(url), category, isCustom)

    val primaryUrl: String
        get() = urls.firstOrNull() ?: ""
}
