package com.example.artshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import android.widget.ImageButton;

public class FullscreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "com.example.artshop.EXTRA_IMAGE_URL";
    private static final String LOG_TAG = "FullscreenImage";

    private PhotoView photoView;
    private ProgressBar progressBar;
    private ImageButton closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);
        hideSystemBars();



        photoView = findViewById(R.id.fullscreen_photoview);
        progressBar = findViewById(R.id.progressBarFullscreen);
        closeButton = findViewById(R.id.buttonCloseFullscreen);

        closeButton.setOnClickListener(v -> finish());

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(LOG_TAG, "Loading image URL: " + imageUrl);
            progressBar.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(imageUrl)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            Log.e(LOG_TAG, "Glide failed to load image.", e);
                            Toast.makeText(FullscreenImageActivity.this, "Kép betöltése sikertelen.", Toast.LENGTH_SHORT).show();
                            finish();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            Log.d(LOG_TAG, "Glide successfully loaded image.");
                            return false;
                        }
                    })
                    .into(photoView);
        } else {
            Log.e(LOG_TAG, "Image URL is missing!");
            Toast.makeText(this, "Kép URL hiányzik.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Allow closing the activity by clicking on the PhotoView background (optional)
        photoView.setOnPhotoTapListener((view, x, y) -> finish());
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }
}