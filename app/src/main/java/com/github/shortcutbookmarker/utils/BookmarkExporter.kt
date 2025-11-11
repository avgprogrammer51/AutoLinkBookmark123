package com.github.shortcutbookmarker.utils

import android.content.Context
import android.os.Environment
import com.github.shortcutbookmarker.models.ChromeShortcut
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BookmarkExporter {
    
    private const val NETSCAPE_BOOKMARK_HEADER = """<!DOCTYPE NETSCAPE-Bookmark-file-1>
<!-- This is an automatically generated file.
     It will be read and overwritten.
     DO NOT EDIT! -->
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<TITLE>Bookmarks</TITLE>
<H1>Bookmarks</H1>
<DL><p>"""

    private const val NETSCAPE_BOOKMARK_FOOTER = """</DL><p>"""
    
    /**
     * Export shortcuts to HTML bookmark file
     */
    fun exportToHtml(
        context: Context,
        shortcuts: List<ChromeShortcut>,
        folderName: String = "Mobile Bookmarks"
    ): Result<File> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "chrome_bookmarks_$timestamp.html"
            val file = File(downloadsDir, fileName)
            
            val html = generateBookmarkHtml(shortcuts, folderName)
            file.writeText(html)
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate HTML content
     */
    private fun generateBookmarkHtml(
        shortcuts: List<ChromeShortcut>,
        folderName: String
    ): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine(NETSCAPE_BOOKMARK_HEADER)
        
        // Folder
        sb.appendLine("    <DT><H3 ADD_DATE=\"${System.currentTimeMillis() / 1000}\" " +
                "LAST_MODIFIED=\"${System.currentTimeMillis() / 1000}\">$folderName</H3>")
        sb.appendLine("    <DL><p>")
        
        // Bookmarks
        shortcuts.forEach { shortcut ->
            val addDate = shortcut.timestamp / 1000
            sb.appendLine("        <DT><A HREF=\"${escapeHtml(shortcut.url)}\" " +
                    "ADD_DATE=\"$addDate\">${escapeHtml(shortcut.title)}</A>")
        }
        
        // Close folder
        sb.appendLine("    </DL><p>")
        
        // Footer
        sb.appendLine(NETSCAPE_BOOKMARK_FOOTER)
        
        return sb.toString()
    }
    
    /**
     * Export to JSON format
     */
    fun exportToJson(
        context: Context,
        shortcuts: List<ChromeShortcut>
    ): Result<File> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "chrome_bookmarks_$timestamp.json"
            val file = File(downloadsDir, fileName)
            
            val json = org.json.JSONArray()
            shortcuts.forEach { shortcut ->
                val obj = org.json.JSONObject()
                obj.put("id", shortcut.id)
                obj.put("title", shortcut.title)
                obj.put("url", shortcut.url)
                obj.put("timestamp", shortcut.timestamp)
                json.put(obj)
            }
            
            file.writeText(json.toString(2))
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Escape HTML special characters
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
