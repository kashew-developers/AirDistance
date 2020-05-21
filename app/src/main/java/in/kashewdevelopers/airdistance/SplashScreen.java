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
        appName = findViewById(R.id.appName);

        animateMarker();

    }

    public void animateMarker() {

        Animation markerAnimation = AnimationUtils
                .loadAnimation(getApplicationContext(), R.anim.fade_in_translate);
        markerAnimation.setFillAfter(true);

        marker.startAnimation(markerAnimation);
        markerPath.startAnimation(markerAnimation);

        markerAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        animatePath();
                    }
                }, 200);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }


    public void animatePath() {

        Animation pathAnimation = AnimationUtils
                .loadAnimation(getApplicationContext(), R.anim.fade_in);
        pathAnimation.setFillAfter(true);

        appName.setVisibility(View.VISIBLE);
        markerPath.setVisibility(View.VISIBLE);
        markerPath.startAnimation(pathAnimation);
        appName.startAnimation(pathAnimation);

        pathAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashScreen.this, MapsActivity.class));
                    }
                }, 200);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }


}
