package com.github.shortcutbookmarker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.shortcutbookmarker.databinding.ActivityMainBinding
import com.github.shortcutbookmarker.models.ChromeShortcut
import com.github.shortcutbookmarker.utils.BookmarkExporter
import com.github.shortcutbookmarker.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = ShortcutAdapter()
    private val shortcuts = mutableListOf<ChromeShortcut>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        checkPermissions()
        updateUI()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.scanButton.setOnClickListener {
            scanForShortcuts()
        }

        binding.exportButton.setOnClickListener {
            showExportDialog()
        }

        binding.startButton.setOnClickListener {
            startBookmarkingProcess()
        }
    }

    private fun checkPermissions() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                PermissionHelper.requestStoragePermission(this)
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun scanForShortcuts() {
        lifecycleScope.launch {
            binding.statusText.text = getString(R.string.scanning_shortcuts)
            binding.progressBar.isIndeterminate = true
            binding.scanButton.isEnabled = false

            val foundShortcuts = withContext(Dispatchers.IO) {
                findChromeShortcuts()
            }

            shortcuts.clear()
            shortcuts.addAll(foundShortcuts)
            adapter.submitList(shortcuts.toList())

            binding.progressBar.isIndeterminate = false
            binding.progressBar.progress = 0
            binding.scanButton.isEnabled = true

            if (shortcuts.isEmpty()) {
                binding.statusText.text = getString(R.string.no_shortcuts_found)
                showNoShortcutsDialog()
            } else {
                binding.statusText.text = getString(R.string.shortcuts_found, shortcuts.size)
            }

            updateUI()
        }
    }

    private fun findChromeShortcuts(): List<ChromeShortcut> {
        val shortcutsList = mutableListOf<ChromeShortcut>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = getSystemService(android.content.pm.LauncherApps::class.java)

                val query = android.content.pm.LauncherApps.ShortcutQuery()
                query.setQueryFlags(
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )

                val shortcuts = launcherApps.getShortcuts(query, android.os.Process.myUserHandle())

                shortcuts?.forEach { shortcut ->
                    val packageName = shortcut.`package`

                    if (packageName.contains("chrome", ignoreCase = true)) {
                        val intent = shortcut.intent
                        val url = intent?.dataString ?: intent?.getStringExtra("url")

                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            shortcutsList.add(
                                ChromeShortcut(
                                    id = shortcut.id,
                                    title = shortcut.shortLabel?.toString()
                                        ?: shortcut.longLabel?.toString()
                                        ?: getString(R.string.untitled),
                                    url = url
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return shortcutsList
    }

    private fun showNoShortcutsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.no_shortcuts_title)
            .setMessage(R.string.no_shortcuts_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showExportDialog() {
        if (shortcuts.isEmpty()) {
            Toast.makeText(this, R.string.scan_first, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.export_format)
            .setItems(arrayOf("HTML (Recommended)", "JSON")) { _, which ->
                when (which) {
                    0 -> exportToHtml()
                    1 -> exportToJson()
                }
            }
            .show()
    }

    private fun exportToHtml() {
        lifecycleScope.launch {
            binding.statusText.text = getString(R.string.exporting)
            binding.exportButton.isEnabled = false

            val result = withContext(Dispatchers.IO) {
                BookmarkExporter.exportToHtml(this@MainActivity, shortcuts)
            }

            binding.exportButton.isEnabled = true

            result.onSuccess { file ->
                binding.statusText.text = getString(R.string.export_success)
                showExportSuccessDialog(file)
            }.onFailure { e ->
                binding.statusText.text = getString(R.string.export_failed)
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportToJson() {
        lifecycleScope.launch {
            binding.statusText.text = getString(R.string.exporting)
            binding.exportButton.isEnabled = false

            val result = withContext(Dispatchers.IO) {
                BookmarkExporter.exportToJson(this@MainActivity, shortcuts)
            }

            binding.exportButton.isEnabled = false

            result.onSuccess { file ->
                binding.statusText.text = getString(R.string.export_success)
                Toast.makeText(
                    this@MainActivity,
                    "Exported to ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { e ->
                binding.statusText.text = getString(R.string.export_failed)
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showExportSuccessDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.export_success)
            .setMessage(getString(R.string.export_success_message, file.absolutePath))
            .setPositiveButton(R.string.share) { _, _ ->
                shareFile(file)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_bookmarks)))
    }

    private fun startBookmarkingProcess() {
        if (shortcuts.isEmpty()) {
            Toast.makeText(this, R.string.scan_first, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.start_processing_title)
            .setMessage(getString(R.string.start_processing_message, shortcuts.size))
            .setPositiveButton(R.string.start) { _, _ ->
                performBookmarking()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performBookmarking() {
        lifecycleScope.launch {
            binding.progressBar.max = shortcuts.size
            binding.progressBar.progress = 0
            binding.progressBar.isIndeterminate = false
            binding.startButton.isEnabled = false
            binding.exportButton.isEnabled = false
            binding.scanButton.isEnabled = false

            shortcuts.forEachIndexed { index, shortcut ->
                binding.statusText.text = getString(
                    R.string.processing_item,
                    index + 1,
                    shortcuts.size,
                    shortcut.title
                )

                try {
                    openInChrome(shortcut)
                    shortcut.isBookmarked = true
                    adapter.submitList(shortcuts.toList())
                    adapter.notifyItemChanged(index)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                binding.progressBar.progress = index + 1
                delay(3000)
            }

            binding.statusText.text = getString(R.string.processing_complete, shortcuts.size)
            binding.startButton.isEnabled = true
            binding.exportButton.isEnabled = true
            binding.scanButton.isEnabled = true

            showCompletionDialog()
        }
    }

    private fun openInChrome(shortcut: ChromeShortcut) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(shortcut.url)
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.url))
            startActivity(fallbackIntent)
        }
    }

    private fun showCompletionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.process_complete)
            .setMessage(R.string.process_complete_message)
            .setPositiveButton(R.string.export_now) { _, _ ->
                exportToHtml()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun updateUI() {
        binding.exportButton.isEnabled = shortcuts.isNotEmpty()
        binding.startButton.isEnabled = shortcuts.isNotEmpty()
    }
}
