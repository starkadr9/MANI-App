package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databinding.ActivityPlaceholderBinding
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.NavigationIcon

class PlaceholderActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityPlaceholderBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(binding.placeholderToolbar, NavigationIcon.Arrow)

        val title = intent.getStringExtra("title") ?: "Placeholder"
        binding.placeholderToolbar.title = title
        
        binding.placeholderTitle.text = title
        binding.placeholderContent.text = """
            This is a placeholder for: $title
            
            To add content here:
            1. Open PlaceholderActivity.kt
            2. Modify the content for this specific menu item
            3. You can add custom logic based on the title parameter
            
            For text content like the Poetic Edda, you can:
            - Store text in assets folder
            - Use a database for searchable content
            - Implement chapter/verse navigation
            - Add bookmarking functionality
        """.trimIndent()

        updateMaterialActivityViews(
            binding.placeholderCoordinator,
            binding.placeholderHolder,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
    }
} 