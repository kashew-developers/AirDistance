package `in`.kashewdevelopers.airdistance.providers

import `in`.kashewdevelopers.airdistance.R
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Animators {
    companion object {
        fun fadeOutView(view: View) {
            view.animate()
                    .alpha(0.0f)
                    .setDuration(Constants.FadeOutDuration)
                    .withEndAction { view.visibility = View.GONE }
        }

        fun fadeInView(view: View) {
            view.visibility = View.VISIBLE
            view.animate()
                    .setDuration(Constants.FadeInDuration)
                    .alpha(1.0f)
        }

        fun rotateControlToggle(controlPanel: View, controlToggle: FloatingActionButton) {
            val newIcon = if (controlPanel.visibility == View.GONE) R.drawable.close_icon
            else R.drawable.keyboard_icon

            val angle = if (controlPanel.visibility == View.GONE) 180f else 0f

            controlToggle.animate()
                    .setDuration(Constants.FadeInDuration)
                    .rotation(angle)
                    .withEndAction { controlToggle.setImageResource(newIcon) }
        }
    }
}