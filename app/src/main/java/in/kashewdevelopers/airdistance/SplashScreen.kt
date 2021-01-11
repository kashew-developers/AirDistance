package `in`.kashewdevelopers.airdistance

import `in`.kashewdevelopers.airdistance.databinding.ActivitySplashScreenBinding
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.markerPath.visibility = View.INVISIBLE
        binding.appName.visibility = View.INVISIBLE

        var animation = AnimationUtils.loadAnimation(this, R.anim.animate_marker)
        binding.marker.startAnimation(animation)

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path)
        binding.markerPath.startAnimation(animation)

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path)
        binding.appName.startAnimation(animation)

        Handler(Looper.getMainLooper())
                .postDelayed({ startActivity(Intent(this@SplashScreen, AirDistance::class.java)) }, 2000)
    }

}
