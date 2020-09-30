package in.kashewdevelopers.airdistance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import in.kashewdevelopers.airdistance.databinding.ActivitySplashScreenBinding;

public class SplashScreen extends AppCompatActivity {

    ActivitySplashScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.markerPath.setVisibility(View.INVISIBLE);
        binding.appName.setVisibility(View.INVISIBLE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.animate_marker);
        binding.marker.startAnimation(animation);

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path);
        binding.markerPath.startAnimation(animation);

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path);
        binding.appName.startAnimation(animation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreen.this, MapsActivity.class));
            }
        }, 2000);
    }

}
