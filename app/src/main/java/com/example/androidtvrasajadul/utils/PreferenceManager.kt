package com.example.androidtvrasajadul.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class to persist user settings and last watched channel state.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "android_tv_rasa_jadul_prefs"
        private const val KEY_LAST_CHANNEL_INDEX = "last_channel_index"
        private const val KEY_LAST_CHANNEL_NUMBER = "last_channel_number"
    }

    /**
     * Save the last played channel index and channel number.
     */
    fun saveLastChannel(index: Int, channelNumber: Int) {
        prefs.edit()
            .putInt(KEY_LAST_CHANNEL_INDEX, index)
            .putInt(KEY_LAST_CHANNEL_NUMBER, channelNumber)
            .apply()
    }

    /**
     * Retrieve the last played channel index (default is 0).
     */
    fun getLastChannelIndex(): Int {
        return prefs.getInt(KEY_LAST_CHANNEL_INDEX, 0)
    }

    /**
     * Retrieve the last played channel number (default is 1).
     */
    fun getLastChannelNumber(): Int {
        return prefs.getInt(KEY_LAST_CHANNEL_NUMBER, 1)
    }
}
