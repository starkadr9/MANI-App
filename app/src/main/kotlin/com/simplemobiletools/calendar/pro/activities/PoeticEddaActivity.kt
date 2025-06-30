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

class PoeticEddaActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityPoeticEddaBinding::inflate)
    private lateinit var adapter: PoeticEddaAdapter
    private var chapters = ArrayList<EddaChapter>()
    
    companion object {
        const val EXTRA_TEXT_FILE = "extra_text_file"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(binding.poeticEddaToolbar, NavigationIcon.Arrow)
        
        // Get title from intent or use default
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Poetic Edda"
        binding.poeticEddaToolbar.title = title
        
        setupRecyclerView()
        loadEddaContent()

        updateMaterialActivityViews(
            binding.poeticEddaCoordinator,
            binding.poeticEddaList,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
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
                // TODO: Implement bookmarks
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = PoeticEddaAdapter(this, chapters) { chapter ->
            // TODO: Open chapter details or expand/collapse
        }
        
        binding.poeticEddaList.apply {
            layoutManager = LinearLayoutManager(this@PoeticEddaActivity)
            adapter = this@PoeticEddaActivity.adapter
        }
    }

    private fun loadEddaContent() {
        try {
            // Get filename from intent or use default
            val filename = intent.getStringExtra(EXTRA_TEXT_FILE) ?: "poeticedda.txt"
            val inputStream = assets.open(filename)
            val content = inputStream.bufferedReader().use { it.readText() }
            chapters.addAll(parseEddaText(content))
        } catch (e: Exception) {
            // Fallback to sample content if file not found
            chapters.add(EddaChapter("Völuspá", "The Prophecy of the Seeress", getSampleVoluspaText()))
            chapters.add(EddaChapter("Hávamál", "The Sayings of the High One", getSampleHavamalText()))
            chapters.add(EddaChapter("Vafþrúðnismál", "The Lay of Vafthrudnir", getSampleVafthrudnismalText()))
            chapters.add(EddaChapter("Grímnismál", "The Lay of Grimnir", getSampleGrimnismalText()))
            chapters.add(EddaChapter("Skírnismál", "The Lay of Skirnir", getSampleSkirnismalText()))
        }
        
        adapter.notifyDataSetChanged()
    }

    private fun parseEddaText(content: String): List<EddaChapter> {
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

    private fun getSampleVoluspaText(): String {
        return """
            1. Hearing I ask from the holy races,
            From Heimdall's sons, both high and low;
            Thou wilt, Valfather, that well I relate
            Old tales I remember of men long ago.

            2. I remember yet the giants of yore,
            Who gave me bread in the days gone by;
            Nine worlds I knew, the nine in the tree
            With mighty roots beneath the mold.

            3. Of old was the age when Ymir lived;
            Sea nor cool waves nor sand there were;
            Earth had not been, nor heaven above,
            But a yawning gap, and grass nowhere.
            
            [This is sample content. Replace with the full Völuspá text]
        """.trimIndent()
    }

    private fun getSampleHavamalText(): String {
        return """
            1. All doorways, before going forward,
            should be looked around,
            should be spied out;
            it's uncertain where enemies
            are sitting ahead in the hall.

            2. Hail to those who give! A guest has come;
            where shall he sit?
            He's in a great hurry, who, by the hearth,
            would test his luck.

            3. Fire he needs who with frozen knees
            has come from the cold outside;
            food and clothes the man requires
            who has fared across the fell.
            
            [This is sample content. Replace with the full Hávamál text]
        """.trimIndent()
    }

    private fun getSampleVafthrudnismalText(): String {
        return """
            [Sample Vafþrúðnismál content]
            
            To add the full text:
            1. Store the complete Poetic Edda text in the assets folder
            2. Create text files for each poem/chapter
            3. Load them dynamically from assets
            4. Implement search across all content
        """.trimIndent()
    }

    private fun getSampleGrimnismalText(): String {
        return """
            [Sample Grímnismál content]
            
            Implementation notes:
            - Each poem should be stored as a separate file
            - Consider adding Old Norse original text alongside translations
            - Implement verse numbering for easy reference
            - Add cross-references between poems
        """.trimIndent()
    }

    private fun getSampleSkirnismalText(): String {
        return """
            [Sample Skírnismál content]
            
            Features to implement:
            - Bookmarking favorite verses
            - Notes and annotations
            - Different translations
            - Audio recitation (if available)
        """.trimIndent()
    }
} 