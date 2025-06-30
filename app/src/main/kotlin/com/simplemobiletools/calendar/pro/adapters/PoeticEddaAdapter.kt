package com.simplemobiletools.calendar.pro.adapters

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.databinding.ItemEddaChapterBinding
import com.simplemobiletools.calendar.pro.models.EddaChapter
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor

class PoeticEddaAdapter(
    private val activity: SimpleActivity,
    private val chapters: ArrayList<EddaChapter>,
    private val itemClick: (EddaChapter) -> Unit
) : RecyclerView.Adapter<PoeticEddaAdapter.ViewHolder>() {

    private val textColor = activity.getProperTextColor()
    private val primaryColor = activity.getProperPrimaryColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEddaChapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.bind(chapter)
    }

    override fun getItemCount() = chapters.size

    inner class ViewHolder(private val binding: ItemEddaChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(chapter: EddaChapter) {
            binding.apply {
                chapterTitle.text = chapter.title
                
                if (chapter.isHeader) {
                    // This is a major category header
                    chapterTitle.setTextColor(primaryColor)
                    chapterTitle.textSize = 18f
                    chapterTitle.setTypeface(null, android.graphics.Typeface.BOLD)
                    chapterSubtitle.visibility = View.GONE
                    chapterContent.visibility = View.GONE
                    expandIcon.visibility = View.GONE
                    
                    // Make non-clickable
                    root.setOnClickListener(null)
                    root.isClickable = false
                } else {
                    // This is a regular collapsible section
                    chapterSubtitle.text = chapter.subtitle
                    chapterTitle.setTextColor(primaryColor)
                    chapterTitle.textSize = 16f
                    chapterTitle.setTypeface(null, android.graphics.Typeface.NORMAL)
                    chapterSubtitle.setTextColor(textColor)
                    chapterContent.setTextColor(textColor)
                    
                    // Show/hide subtitle based on content
                    if (chapter.subtitle.isNotEmpty()) {
                        chapterSubtitle.visibility = View.VISIBLE
                    } else {
                        chapterSubtitle.visibility = View.GONE
                    }
                    
                    expandIcon.visibility = View.VISIBLE

                    if (chapter.isExpanded) {
                        chapterContent.text = chapter.content
                        chapterContent.visibility = View.VISIBLE
                        expandIcon.rotation = 180f
                        
                        // Make links clickable
                        Linkify.addLinks(chapterContent, Linkify.WEB_URLS)
                        chapterContent.movementMethod = LinkMovementMethod.getInstance()
                    } else {
                        chapterContent.visibility = View.GONE
                        expandIcon.rotation = 0f
                    }

                    root.setOnClickListener {
                        chapter.isExpanded = !chapter.isExpanded
                        notifyItemChanged(adapterPosition)
                        itemClick(chapter)
                    }
                    root.isClickable = true
                }
            }
        }
    }
} 