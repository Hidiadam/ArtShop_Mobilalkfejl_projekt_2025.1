package com.example.artshop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem; // Vissza gombhoz
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.artshop.logic.CartManager;
import com.example.artshop.model.ArtworkItem;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.PhotoViewAttacher; // Needed for matrix access
import android.graphics.Matrix;
import android.view.MotionEvent; // Make sure this is imported
import android.view.View; // Make sure this is imported
import android.view.ViewTreeObserver; // Make sure this is imported


public class ArtworkDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = ArtworkDetailActivity.class.getSimpleName();
    public static final String EXTRA_DETAIL_ARTWORK_ID = "com.example.artshop.EXTRA_DETAIL_ARTWORK_ID";

    // UI Elemek

    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private RatingBar mRatingBar;
    private TextView mPriceTextView;
    private TextView mDescriptionTextView;
    private Button mAddToCartButton;
    private ProgressBar mProgressBar;

    // Firebase
    private FirebaseFirestore mFirestore;
    private CollectionReference mArtworkCollection;
    private PhotoView mImageView; // Ensure this is PhotoView type
    private Matrix defaultMatrix; // To store the initial matrix


    private String mArtworkId;
    private ArtworkItem mCurrentArtwork; // A betöltött műalkotás

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_detail);

        // Vissza gomb engedélyezése az ActionBar-on (ha van)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        // UI Elemek inicializálása
        mImageView = findViewById(R.id.imageViewDetail);
        mTitleTextView = findViewById(R.id.textViewDetailTitle);
        mArtistTextView = findViewById(R.id.textViewDetailArtist);
        mRatingBar = findViewById(R.id.ratingBarDetail);
        mPriceTextView = findViewById(R.id.textViewDetailPrice);
        mDescriptionTextView = findViewById(R.id.textViewDetailDescription);
        mAddToCartButton = findViewById(R.id.buttonDetailAddToCart);
        mProgressBar = findViewById(R.id.progressBarDetail);


        // Firebase
        mFirestore = FirebaseFirestore.getInstance();
        mArtworkCollection = mFirestore.collection("artworks");

        // ID lekérése az Intentből
        mArtworkId = getIntent().getStringExtra(EXTRA_DETAIL_ARTWORK_ID);

        if (mArtworkId == null || mArtworkId.isEmpty()) {
            Log.e(LOG_TAG, "Artwork ID is missing!");
            Toast.makeText(this, "Hiba: Műalkotás azonosító hiányzik.", Toast.LENGTH_LONG).show();
            finish(); // Bezárjuk az Activity-t, ha nincs ID
            return;
        }

        Log.d(LOG_TAG, "Loading details for Artwork ID: " + mArtworkId);
        loadArtworkDetails();

        // Kosárba gomb listener
        mAddToCartButton.setOnClickListener(v -> addToCart());

        mImageView.setOnClickListener(v -> {
            if (mCurrentArtwork != null && mCurrentArtwork.getImageUrl() != null && !mCurrentArtwork.getImageUrl().isEmpty()) {
                Intent intent = new Intent(ArtworkDetailActivity.this, FullscreenImageActivity.class);
                intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, mCurrentArtwork.getImageUrl());
                startActivity(intent);
            } else {
                Log.w(LOG_TAG, "Cannot open fullscreen image, URL is missing.");
                Toast.makeText(this, "Kép nem érhető el.", Toast.LENGTH_SHORT).show();
            }
        });
        setupPhotoViewResetListener();
    }

    private void setupPhotoViewResetListener() {
        final PhotoViewAttacher attacher = mImageView.getAttacher();
        if (attacher == null) {
            Log.e(LOG_TAG, "PhotoViewAttacher is null, cannot setup reset listener.");
            return;
        }

        // Track if user is currently zooming
        final boolean[] isCurrentlyZooming = {false};

        // Store the initial matrix state after the image is loaded
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (defaultMatrix == null) {
                            defaultMatrix = new Matrix();
                            attacher.getDisplayMatrix(defaultMatrix);
                            Log.d(LOG_TAG, "Default PhotoView matrix stored.");
                        }
                    }
                }
        );

        // Listen for scale changes to detect zooming
        attacher.setOnScaleChangeListener(new OnScaleChangedListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                isCurrentlyZooming[0] = attacher.getScale() > attacher.getMinimumScale();
            }
        });

        // Handle touch events to reset zoom if needed
        mImageView.setOnTouchListener((v, event) -> {
            boolean handled = attacher.onTouch(v, event);

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {

                if (isCurrentlyZooming[0]) {
                    attacher.setScale(attacher.getMinimumScale(), true);
                    isCurrentlyZooming[0] = false;
                    Log.d(LOG_TAG, "Resetting zoom after pinch gesture");
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick(); // For accessibility
                }
            }
            return handled;
        });

        // Optional: handle tap events
        attacher.setOnPhotoTapListener((view, x, y) -> {
            Log.v(LOG_TAG, "PhotoView tapped.");
            // Handle tap events here if needed
        });
    }


    // Adatok betöltése Firestore-ból
    private void loadArtworkDetails() {
        mProgressBar.setVisibility(View.VISIBLE);
        mArtworkCollection.document(mArtworkId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        mCurrentArtwork = documentSnapshot.toObject(ArtworkItem.class);
                        if (mCurrentArtwork != null) {
                            mCurrentArtwork.setId(documentSnapshot.getId()); // ID beállítása
                            populateUI();
                            Log.i(LOG_TAG, "Artwork details loaded successfully.");
                        } else {
                            handleLoadError("Hiba az adatok feldolgozásakor.");
                        }
                    } else {
                        handleLoadError("Műalkotás nem található.");
                    }
                })
                .addOnFailureListener(e -> {
                    mProgressBar.setVisibility(View.GONE);
                    Log.e(LOG_TAG, "Error loading artwork details", e);
                    handleLoadError("Hiba a betöltés során: " + e.getMessage());
                });
    }

    // Hiba kezelése betöltéskor
    private void handleLoadError(String message) {
        Log.e(LOG_TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        mAddToCartButton.setEnabled(false);
    }

    // UI feltöltése az adatokkal
    private void populateUI() {
        if (mCurrentArtwork == null) return;

        setTitle(""); // Activity címének beállítása
        mTitleTextView.setText(mCurrentArtwork.getTitle());
        mArtistTextView.setText(getString(R.string.artist_label, mCurrentArtwork.getArtist())); // String erőforrás használata
        mRatingBar.setRating(mCurrentArtwork.getRating());
        mDescriptionTextView.setText(mCurrentArtwork.getDescription());

        // Ár formázása (Egyszerű Ft hozzáadás)
        String formattedPrice;
        long price = mCurrentArtwork.getPrice();

        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("hu", "HU"));
            symbols.setGroupingSeparator(' '); // Already default for hu_HU, but explicit doesn't hurt
            DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
            formattedPrice = formatter.format(price) + " Ft";

        } catch (NumberFormatException | NullPointerException e) {
            Log.e(LOG_TAG, "Error formatting price: ", e);
            formattedPrice = " (Hiba)";
        }

        mPriceTextView.setText(formattedPrice);

        // Kép betöltése Glide-dal
        Glide.with(this)
                .load(mCurrentArtwork.getImageUrl())
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_broken_image_24)
                .fallback(R.drawable.baseline_search_24)
                .into(mImageView);
    }

    // Elem hozzáadása a kosárhoz
    private void addToCart() {
        if (mCurrentArtwork != null) {
            CartManager.getInstance().addItem(mCurrentArtwork);
            Toast.makeText(this, "'" + mCurrentArtwork.getTitle() + "' a kosárba helyezve!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Log.e(LOG_TAG, "Cannot add to cart, mCurrentArtwork is null.");
            Toast.makeText(this, "Hiba történt a kosárba helyezéskor.", Toast.LENGTH_SHORT).show();
        }
    }

    // ActionBar vissza gomb kezelése
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}