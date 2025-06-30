package com.simplemobiletools.calendar.pro.adapters

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
                chapterSubtitle.text = chapter.subtitle
                chapterTitle.setTextColor(primaryColor)
                chapterSubtitle.setTextColor(textColor)
                chapterContent.setTextColor(textColor)

                if (chapter.isExpanded) {
                    chapterContent.text = chapter.content
                    chapterContent.visibility = View.VISIBLE
                    expandIcon.rotation = 180f
                } else {
                    chapterContent.visibility = View.GONE
                    expandIcon.rotation = 0f
                }

                root.setOnClickListener {
                    chapter.isExpanded = !chapter.isExpanded
                    notifyItemChanged(adapterPosition)
                    itemClick(chapter)
                }
            }
        }
    }
} 