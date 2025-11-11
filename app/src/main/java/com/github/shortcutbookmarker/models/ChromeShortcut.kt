package com.github.shortcutbookmarker.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a Chrome shortcut
 */
@Parcelize
data class ChromeShortcut(
    val id: String,
    val title: String,
    val url: String,
    var isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Get domain from URL
     */
    fun getDomain(): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Check if URL is valid
     */
    fun isValidUrl(): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
