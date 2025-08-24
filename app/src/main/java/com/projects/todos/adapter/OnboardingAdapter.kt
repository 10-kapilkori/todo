package com.projects.todos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.projects.todos.R
import com.projects.todos.data.OnboardingItem

class OnboardingAdapter(
    private val onboardingItems: List<OnboardingItem>,
    private val onGetStartedClick: () -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.onboardingImage)
        private val titleTextView: TextView = itemView.findViewById(R.id.onboardingTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.onboardingDescription)
        private val getStartedButton: TextView = itemView.findViewById(R.id.getStartedButton)

        fun bind(item: OnboardingItem, position: Int) {
            titleTextView.text = item.title
            descriptionTextView.text = item.description
            
            // Load image from resources - try drawable first, then mipmap
            var imageResId = itemView.context.resources.getIdentifier(
                item.image, "drawable", itemView.context.packageName
            )
            
            // If not found in drawable, try mipmap
            if (imageResId == 0) {
                imageResId = itemView.context.resources.getIdentifier(
                    item.image, "mipmap", itemView.context.packageName
                )
            }
            
            if (imageResId != 0) {
                imageView.setImageResource(imageResId)
                // Remove tint for PNG images
                imageView.imageTintList = null
            }
            
            // Show "Get Started" button only on the last slide
            if (position == onboardingItems.size - 1) {
                getStartedButton.visibility = View.VISIBLE
                getStartedButton.setOnClickListener { 
                    // Add scale animation on button click
                    getStartedButton.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {
                            getStartedButton.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .withEndAction {
                                    onGetStartedClick()
                                }
                                .start()
                        }
                        .start()
                }
            } else {
                getStartedButton.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_slide, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position], position)
    }

    override fun onViewAttachedToWindow(holder: OnboardingViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Add entrance animation for onboarding slides
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_entrance)
        )
    }

    override fun onViewDetachedFromWindow(holder: OnboardingViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    override fun getItemCount(): Int = onboardingItems.size
}
