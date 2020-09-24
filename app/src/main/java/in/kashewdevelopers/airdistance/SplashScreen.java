package in.kashewdevelopers.airdistance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreen extends AppCompatActivity {

    ImageView marker, markerPath;
    TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        marker = findViewById(R.id.marker);
        markerPath = findViewById(R.id.markerPath);
        markerPath.setVisibility(View.INVISIBLE);
        appName = findViewById(R.id.appName);
        appName.setVisibility(View.INVISIBLE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.animate_marker);
        marker.startAnimation(animation);

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path);
        markerPath.startAnimation(animation);

        animation = AnimationUtils.loadAnimation(this, R.anim.animate_path);
        appName.startAnimation(animation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreen.this, MapsActivity.class));
            }
        }, 2000);
    }

}
