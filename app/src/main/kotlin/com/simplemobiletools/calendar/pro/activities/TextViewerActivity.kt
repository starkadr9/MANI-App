package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.PoeticEddaAdapter
import com.simplemobiletools.calendar.pro.databinding.ActivityPoeticEddaBinding
import com.simplemobiletools.calendar.pro.models.EddaChapter
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.NavigationIcon

class TextViewerActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityPoeticEddaBinding::inflate)
    private lateinit var adapter: PoeticEddaAdapter
    private val chapters = ArrayList<EddaChapter>()

    companion object {
        const val EXTRA_TEXT_FILE = "text_file"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        val textFile = intent.getStringExtra(EXTRA_TEXT_FILE) ?: ""
        val titleText = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        
        setupToolbar(binding.poeticEddaToolbar, NavigationIcon.Arrow)
        supportActionBar?.title = titleText
        setupRecyclerView()
        loadTextContent(textFile)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_poetic_edda, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_edda -> {
                // TODO: Implement search functionality
                true
            }
            R.id.bookmarks -> {
                // TODO: Implement bookmarks functionality
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = PoeticEddaAdapter(this, chapters) { chapter ->
            chapter.isExpanded = !chapter.isExpanded
            adapter.notifyDataSetChanged()
        }
        
        binding.poeticEddaList.apply {
            layoutManager = LinearLayoutManager(this@TextViewerActivity)
            adapter = this@TextViewerActivity.adapter
        }
    }

    private fun loadTextContent(fileName: String) {
        if (fileName.isEmpty()) {
            chapters.add(EddaChapter("Error", "No file specified", "No text file was specified to load."))
            adapter.notifyDataSetChanged()
            return
        }
        
        try {
            val inputStream = assets.open(fileName)
            val content = inputStream.bufferedReader().use { it.readText() }
            chapters.addAll(parseTextContent(content))
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            chapters.add(EddaChapter("Error", "File not found", "Could not load the text file: $fileName\n\nError: ${e.message}"))
            adapter.notifyDataSetChanged()
        }
    }

    private fun parseTextContent(content: String): List<EddaChapter> {
        val chapters = mutableListOf<EddaChapter>()
        val lines = content.lines()
        
        var currentTitle = ""
        var currentSubtitle = ""
        var currentContent = StringBuilder()
        var inHeader = false
        var headerLineCount = 0
        
        for (line in lines) {
            when {
                line.trim() == "--" -> {
                    if (inHeader) {
                        // End of header section, start collecting content
                        inHeader = false
                        headerLineCount = 0
                    } else {
                        // Save previous chapter if exists
                        if (currentTitle.isNotEmpty()) {
                            chapters.add(EddaChapter(currentTitle, currentSubtitle, currentContent.toString().trim()))
                        }
                        
                        // Start of new header section
                        inHeader = true
                        headerLineCount = 0
                        currentTitle = ""
                        currentSubtitle = ""
                        currentContent = StringBuilder()
                    }
                }
                inHeader -> {
                    // We're inside a header section
                    when (headerLineCount) {
                        0 -> {
                            if (line.trim().isNotEmpty()) {
                                currentTitle = line.trim()
                                headerLineCount++
                            }
                        }
                        1 -> {
                            if (line.trim().isNotEmpty()) {
                                currentSubtitle = line.trim()
                                headerLineCount++
                            }
                        }
                        // Ignore any additional lines in header until closing --
                    }
                }
                !inHeader && currentTitle.isNotEmpty() -> {
                    // We're collecting content for the current chapter
                    currentContent.append(line).append("\n")
                }
            }
        }
        
        // Don't forget the last chapter
        if (currentTitle.isNotEmpty()) {
            chapters.add(EddaChapter(currentTitle, currentSubtitle, currentContent.toString().trim()))
        }
        
        return chapters
    }
} 