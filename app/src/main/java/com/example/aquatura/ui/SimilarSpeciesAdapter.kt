package com.example.aquatura.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aquatura.R
import com.example.aquatura.data.Prediction

class SimilarSpeciesAdapter(
    private val similarFish: List<Prediction>,
    private val onItemClick: (Prediction) -> Unit
) : RecyclerView.Adapter<SimilarSpeciesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.similarFishImage)
        val name: TextView = view.findViewById(R.id.similarFishName)
        val similarity: TextView = view.findViewById(R.id.similarityPercentage)

        fun bind(fish: Prediction) {
            image.setImageResource(getFishImageResource(fish.classIndex))
            name.text = fish.fishName
            similarity.text = "${(fish.confidence * 100).toInt()}% match"
            
            itemView.setOnClickListener { onItemClick(fish) }
        }
        
        private fun getFishImageResource(index: Int): Int {
            val resourceName = "fish_$index"
            return try {
                itemView.context.resources.getIdentifier(resourceName, "drawable", itemView.context.packageName)
            } catch (e: Exception) {
                R.drawable.ic_fish_outline
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_similar_fish, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(similarFish[position])
    }

    override fun getItemCount() = similarFish.size
}
