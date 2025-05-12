package com.example.artshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder; // Újabb API-khoz
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.artshop.model.ArtworkItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.collection.BuildConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID; // Véletlen fájlnévhez

import android.content.res.Resources; // Import Resources
import android.widget.ImageView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions; // Merge opcióhoz
import java.util.HashMap; // Map létrehozásához
import java.util.Map; // Map típushoz



public class AddEditArtworkActivity extends AppCompatActivity {

    private static final String LOG_TAG = AddEditArtworkActivity.class.getName();
    public static final String EXTRA_ARTWORK_ID = "com.example.artshop.EXTRA_ARTWORK_ID";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102; // Ha galériából is kell olvasni
    //képtömörítés
    private static final long MAX_IMAGE_BYTES = 25L * 1024 * 1024;  // 25 MB
    private static final int  MAX_DIMENSION   = 1920;               // px

    // UI Elemek
    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mDescriptionEditText;
    private EditText mPriceEditText;
    private RatingBar mRatingBar;
    private ImageView mArtworkImageView;
    private Button mSaveButton;
    private FloatingActionButton mSelectImageFab;
    private FloatingActionButton mCaptureImageFab;
    private ProgressBar mProgressBarUpload; // Feltöltés jelző
    private int placeholderPaddingPx;

    // Firebase
    private FirebaseFirestore mFirestore;
    private CollectionReference mArtworkCollection;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private FirebaseUser mCurrentUser;


    // Állapot
    private String mCurrentArtworkId = null; // Ha ez null, akkor 'Add', különben 'Edit' mód
    private ArtworkItem mEditingArtwork = null; // Szerkesztett elem adatai
    private Uri mImageUri = null; // A kiválasztott/készített kép URI-ja
    private String mUploadedImageUrl = null; // A feltöltött kép URL-je
    private String mCurrentPhotoPath; // Kamera által készített kép elérési útja

    private String mOriginalArtistName = null;

    // --- ActivityResult Launcherek ---
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    mImageUri = result.getData().getData();
                    Log.d(LOG_TAG, "Image selected from gallery: " + mImageUri);
                    displayArtworkImage(mImageUri);
                    mUploadedImageUrl = null; // Töröljük a régi feltöltött URL-t, ha újat választ
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    // A kép az mImageUri által mutatott helyre került (amit a createImageFile adott meg)
                    Log.d(LOG_TAG, "Image captured successfully to: " + mImageUri);
                    displayArtworkImage(mImageUri);
                    mUploadedImageUrl = null; // Töröljük a régi feltöltött URL-t
                } else {
                    Log.e(LOG_TAG, "Image capture failed or was cancelled.");

                    if (mImageUri != null) {


                        try {
                            File photoFile = new File(mImageUri.getPath());
                            if (photoFile.exists()) {
                                // photoFile.delete();
                                Log.w(LOG_TAG, "Temporary image file exists but capture failed: " + mImageUri.getPath());
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error handling temporary file after failed capture", e);
                        }
                        mImageUri = null;
                    }
                    Toast.makeText(this, "Kép készítése sikertelen vagy megszakítva.", Toast.LENGTH_SHORT).show();
                    mImageUri = null;
                }
            });

    // Engedélykérés Launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                //boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean isCameraPermissionGranted = permissions.get(Manifest.permission.CAMERA);
                boolean cameraGranted = isCameraPermissionGranted != null && isCameraPermissionGranted;
                // WRITE_EXTERNAL_STORAGE
                // boolean storageGranted = permissions.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);

                if (cameraGranted) {
                    Log.d(LOG_TAG, "Camera permission granted.");
                    // Ha van kamera engedély, megpróbálhatjuk újra a kamera indítását
                    // De egyszerűbb a felhasználóra bízni, hogy újra kattintson.
                    Toast.makeText(this, "Kamera engedély megadva.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(LOG_TAG, "Camera permission was denied.");
                    Toast.makeText(this, "Kamera engedély szükséges a kép készítéséhez.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_artwork);

        // UI Elemek inicializálása
        mTitleEditText = findViewById(R.id.editTextArtworkTitle);
        mArtistEditText = findViewById(R.id.editTextArtworkArtist);
        mDescriptionEditText = findViewById(R.id.editTextArtworkDescription);
        mPriceEditText = findViewById(R.id.editTextArtworkPrice);
        mRatingBar = findViewById(R.id.ratingBarArtwork);
        mArtworkImageView = findViewById(R.id.imageViewArtworkPreview);
        mSaveButton = findViewById(R.id.buttonSaveArtwork);
        mSelectImageFab = findViewById(R.id.fabSelectImage);
        mCaptureImageFab = findViewById(R.id.fabCaptureImage);
        mProgressBarUpload = findViewById(R.id.progressBarUpload);

        // Firebase inicializálása
        mFirestore = FirebaseFirestore.getInstance();
        mArtworkCollection = mFirestore.collection("artworks");
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference(); // Gyökér referencia
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        placeholderPaddingPx = getResources().getDimensionPixelSize(R.dimen.artwork_placeholder_padding);

        if (mCurrentUser == null) {
            Log.e(LOG_TAG, "User not logged in! Cannot add/edit artwork.");
            Toast.makeText(this, "Hiba: Nincs bejelentkezett felhasználó.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Intent adatok ellenőrzése (szerkesztés mód?)
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ARTWORK_ID)) {
            mCurrentArtworkId = intent.getStringExtra(EXTRA_ARTWORK_ID);
            setTitle("Műalkotás Szerkesztése"); // Cím módosítása
            Log.d(LOG_TAG, "Edit mode for ID: " + mCurrentArtworkId);
            loadArtworkData(); // Adatok betöltése szerkesztéshez
        } else {
            setTitle("Új Műalkotás Hozzáadása");
            Log.d(LOG_TAG, "Add mode.");
            // Új elem esetén az értékelést pl. 0-ra állíthatjuk
            mRatingBar.setRating(0);
            mRatingBar.setIsIndicator(false); // Engedélyezzük a szerkesztést
            resetToPlaceholder();
        }

        // Gomb eseménykezelők
        mSaveButton.setOnClickListener(v -> saveArtwork());
        mSelectImageFab.setOnClickListener(v -> selectImageFromGallery());
        mCaptureImageFab.setOnClickListener(v -> captureImageWithCamera());

        // Animáció indítása (Második kötelező animáció)
        // Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.custom_fade_in);
        findViewById(R.id.addEditScrollView).startAnimation(fadeIn); // A gyökér elemen

        Log.i(LOG_TAG, "onCreate finished with custom animation."); // Log frissítve
    }

    // --- NEW HELPER METHOD: Reset ImageView to Placeholder State ---
    private void resetToPlaceholder() {
        Log.d(LOG_TAG, "Resetting ImageView to placeholder state.");
        mArtworkImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mArtworkImageView.setPadding(placeholderPaddingPx, placeholderPaddingPx, placeholderPaddingPx, placeholderPaddingPx);
        Glide.with(this)
                .load(R.drawable.baseline_add_photo_alternate_24) // Placeholder icon
                .into(mArtworkImageView);
    }

    // --- NEW HELPER METHOD: Display Artwork Image (from Uri or URL) ---
    private void displayArtworkImage(Object imageSource) {
        if (imageSource == null) {
            resetToPlaceholder();
            return;
        }
        Log.d(LOG_TAG, "Displaying artwork image. Source type: " + imageSource.getClass().getSimpleName());
        mArtworkImageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Fill the view
        mArtworkImageView.setPadding(0, 0, 0, 0);  // Remove padding

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_broken_image_24)
                .into(mArtworkImageView);
    }

    // Adatok betöltése Firestore-ból szerkesztés módban
    private void loadArtworkData() {
        if (mCurrentArtworkId == null) return;

        mProgressBarUpload.setVisibility(View.VISIBLE); // Töltés jelzése
        mArtworkCollection.document(mCurrentArtworkId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    mProgressBarUpload.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        mEditingArtwork = documentSnapshot.toObject(ArtworkItem.class);
                        if (mEditingArtwork != null) {
                            mEditingArtwork.setId(documentSnapshot.getId());
                            mOriginalArtistName = mEditingArtwork.getArtist();
                            populateUI();
                            Log.d(LOG_TAG, "Artwork data loaded for editing.");
                        } else {
                            Log.e(LOG_TAG, "Failed to convert document to ArtworkItem.");
                            Toast.makeText(this, "Hiba a műalkotás adatainak betöltésekor.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(LOG_TAG, "Artwork document not found for ID: " + mCurrentArtworkId);
                        Toast.makeText(this, "A szerkesztendő műalkotás nem található.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    mProgressBarUpload.setVisibility(View.GONE);
                    Log.e(LOG_TAG, "Error loading artwork data for ID: " + mCurrentArtworkId, e);
                    Toast.makeText(this, "Hiba a betöltéskor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    // UI feltöltése a betöltött adatokkal
    private void populateUI() {
        if (mEditingArtwork == null) return;

        mTitleEditText.setText(mEditingArtwork.getTitle());
        mArtistEditText.setText(mEditingArtwork.getArtist());
        mDescriptionEditText.setText(mEditingArtwork.getDescription());
        mPriceEditText.setText(String.valueOf(mEditingArtwork.getPrice()));
        mRatingBar.setRating(mEditingArtwork.getRating());
        mRatingBar.setIsIndicator(false); // Engedélyezzük a szerkesztést

        mUploadedImageUrl = mEditingArtwork.getImageUrl();
        if (mUploadedImageUrl != null && !mUploadedImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(mUploadedImageUrl)
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_broken_image_24)
                    .into(mArtworkImageView);
        } else {
            mArtworkImageView.setImageResource(R.drawable.baseline_add_photo_alternate_24); // Placeholder, ha nincs kép
        }
        displayArtworkImage(mUploadedImageUrl);
    }

    // Kép kiválasztása galériából
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Opcionális: Csak képeket engedélyez
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // Kép készítése kamerával
    private void captureImageWithCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Camera permission not granted, requesting...");
            // Engedély kérése
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
            // WRITE_EXTERNAL_STORAGE
            // requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        } else {
            Log.d(LOG_TAG, "Camera permission already granted, launching camera.");
            launchCameraIntent();
        }
    }

    private void launchCameraIntent() {
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error creating image file", ex);
            Toast.makeText(this, "Hiba a képfájl létrehozásakor.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            try {
                // Use FileProvider with explicit authority string
                mImageUri = FileProvider.getUriForFile(this,
                        "com.example.artshop.fileprovider",
                        photoFile);

                Log.d(LOG_TAG, "FileProvider URI created: " + mImageUri);

                // Create camera intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);

                // Grant permissions
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, mImageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Log.d(LOG_TAG, "Granted permission to: " + packageName);
                }

                // Log if any camera apps were found
                if (resInfoList.isEmpty()) {
                    Log.e(LOG_TAG, "No camera apps found on device!");
                    Toast.makeText(this, "Nem található kamera alkalmazás a készüléken.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Launch camera directly
                Log.d(LOG_TAG, "Launching camera with URI: " + mImageUri);
                cameraLauncher.launch(mImageUri);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error launching camera", e);
                Toast.makeText(this, "Hiba a kamera indításakor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Segédfüggvény képfájl létrehozására
    private File createImageFile() throws IOException {
        // Hozz létre egy egyedi képfájl nevet
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            Log.e(LOG_TAG, "External files directory is null.");
            throw new IOException("External files directory not available");
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Mentsd el a fájl elérési útját:
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(LOG_TAG, "Image file created at: " + mCurrentPhotoPath);
        return image;
    }


    // Műalkotás mentése (új vagy szerkesztett)
    private void saveArtwork() {
        String title       = mTitleEditText.getText().toString().trim();
        String artist      = mArtistEditText.getText().toString().trim();
        String description = mDescriptionEditText.getText().toString().trim();
        String priceStr    = mPriceEditText.getText().toString().trim().replace(" ", "");
        float  rating      = mRatingBar.getRating();

        // Kötelező mezők ellenőrzése
        if (title.isEmpty() || artist.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "A Cím, Alkotó és Ár mezők kitöltése kötelező!", Toast.LENGTH_LONG).show();
            return;
        }

        // Ár átalakítása egész számra (Ft)
        long price;
        try {
            price = Long.parseLong(priceStr);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Az árnak érvényes, pozitív egész számnak kell lennie!", Toast.LENGTH_LONG).show();
            return;
        }

        mProgressBarUpload.setVisibility(View.VISIBLE);
        mSaveButton.setEnabled(false);

        // --- képkezelés ---
        if (mImageUri != null) {
            if (!isImageSizeOk(mImageUri)) {
                mProgressBarUpload.setVisibility(View.GONE);
                mSaveButton.setEnabled(true);
                Toast.makeText(this, "A kép túl nagy (max 25 MB)!", Toast.LENGTH_LONG).show();
                return;
            }
            if (mCurrentArtworkId != null && mEditingArtwork != null &&
                    mEditingArtwork.getImageUrl() != null && !mEditingArtwork.getImageUrl().isEmpty()) {
                deleteOldImage(mEditingArtwork.getImageUrl());
            }
            uploadImageAndSaveData(title, artist, description, price, rating);
        } else {
            saveDataToFirestore(title, artist, description, price, rating, mUploadedImageUrl);
        }
    }

    private boolean isImageSizeOk(Uri uri) {
        try (android.database.Cursor c = getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                long size = (idx >= 0) ? c.getLong(idx) : 0;
                return size <= MAX_IMAGE_BYTES;
            }
        } catch (Exception e) { Log.e(LOG_TAG, "Size check error", e); }
        return false;
    }

    private byte[] getCompressedBytes(Uri uri) throws IOException {
        Bitmap src;
        if (Build.VERSION.SDK_INT >= 29) {
            src = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            src = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }

        int w = src.getWidth(), h = src.getHeight();
        float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(w, h));
        Bitmap resized = Bitmap.createScaledBitmap(src,
                (int) (w * scale), (int) (h * scale), true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 80, out); // 80% minőség

        src.recycle();
        if (resized != src) resized.recycle();

        return out.toByteArray();
    }


    // Régi kép törlése Storage-ból szerkesztéskor (opcionális)
    private void deleteOldImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(LOG_TAG, "No old image URL to delete.");
            return;
        }
        try {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            imageRef.delete().addOnSuccessListener(aVoid ->
                    Log.d(LOG_TAG, "Old associated image deleted from Storage: " + imageUrl)
            ).addOnFailureListener(e ->
                    Log.e(LOG_TAG, "Error deleting old image from Storage: " + imageUrl, e)
            );
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Invalid old image URL, cannot delete from Storage: " + imageUrl, e);
        }
    }


    // Kép feltöltése Firebase Storage-ba, majd adatok mentése Firestore-ba
    private void uploadImageAndSaveData(String title, String artist, String description, long price, float rating) {
        if (mImageUri == null) {
            // Ha valamiért mégis null az URI, mentsük kép nélkül
            saveDataToFirestore(title, artist, description, price, rating, null);
            return;
        }

        // Egyedi fájlnév generálása a Storage-ban
        String filename = "artworks/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = mStorageRef.child(filename);

        Log.d(LOG_TAG, "Uploading image to: " + fileRef.getPath());


        byte[] imgBytes;
        try {
            imgBytes = getCompressedBytes(mImageUri); // tömörítés
        } catch (IOException e) {
            Log.e(LOG_TAG, "Image compress error", e);
            Toast.makeText(this, "Hiba képtömörítés közben.", Toast.LENGTH_LONG).show();
            mProgressBarUpload.setVisibility(View.GONE);
            mSaveButton.setEnabled(true);
            return;
        }


        fileRef.putBytes(imgBytes)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            mUploadedImageUrl = uri.toString();
                            saveDataToFirestore(title, artist, description, price, rating, mUploadedImageUrl);
                        }).addOnFailureListener(e -> {
                            Log.e(LOG_TAG, "Error getting download URL", e);
                            mProgressBarUpload.setVisibility(View.GONE);
                            mSaveButton.setEnabled(true);
                            Toast.makeText(AddEditArtworkActivity.this, "Hiba a kép URL lekérésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error uploading image", e);
                    mProgressBarUpload.setVisibility(View.GONE);
                    mSaveButton.setEnabled(true);
                    Toast.makeText(AddEditArtworkActivity.this, "Hiba a kép feltöltésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(LOG_TAG, "Upload progress: " + String.format("%.0f%%", progress));
                });
    }


    // Adatok mentése Firestore-ba (új elem hozzáadása vagy meglévő frissítése)
    private void saveDataToFirestore(String title, String artist, String description, long price, float rating, String imageUrl) {

        if (mCurrentUser == null) {
            Log.e(LOG_TAG, "User is null, cannot save data.");
            mProgressBarUpload.setVisibility(View.GONE);
            mSaveButton.setEnabled(true);
            Toast.makeText(this, "Hiba: Felhasználó nincs bejelentkezve.", Toast.LENGTH_SHORT).show();

            return;
        }


        final String newArtistName = (artist != null) ? artist.trim() : null; // Tisztított új név
        final String oldArtistName = mOriginalArtistName; // Régi név (lehet null, ha új elemet adunk hozzá)
        ArtworkItem artwork = new ArtworkItem(title, artist, description, price, rating, imageUrl, mCurrentUser.getUid());

        // Döntés: Hozzáadás vagy Frissítés
        if (mCurrentArtworkId == null) { // Hozzáadás (Create)
            Log.d(LOG_TAG, "Saving new artwork to Firestore.");
            mArtworkCollection.add(artwork)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(LOG_TAG, "New artwork added with ID: " + documentReference.getId());
                        addArtistToCollectionIfNeeded(newArtistName);
                        mProgressBarUpload.setVisibility(View.GONE);
                        // Visszaadjuk az OK eredményt az előző Activity-nek
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(LOG_TAG, "Error adding new artwork", e);
                        mProgressBarUpload.setVisibility(View.GONE);
                        mSaveButton.setEnabled(true);
                        Toast.makeText(AddEditArtworkActivity.this, "Hiba a mentés során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else { // Frissítés (Update)
            Log.d(LOG_TAG, "Updating artwork with ID: " + mCurrentArtworkId);
            mArtworkCollection.document(mCurrentArtworkId).set(artwork)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(LOG_TAG, "Artwork updated successfully: " + mCurrentArtworkId);
                        addArtistToCollectionIfNeeded(newArtistName);
                        boolean artistChanged = oldArtistName != null && newArtistName != null && !oldArtistName.equals(newArtistName);
                        artistChanged = artistChanged || (oldArtistName == null && newArtistName != null) || (oldArtistName != null && newArtistName == null);
                        if (artistChanged && oldArtistName != null && !oldArtistName.isEmpty()) {
                            Log.d(LOG_TAG,"Artist name changed from '"+oldArtistName+"' to '"+newArtistName+"'. Checking if old artist needs deletion.");
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("CHECK_ARTIST_DELETION", oldArtistName);
                            setResult(Activity.RESULT_OK, resultIntent);
                        } else {
                            setResult(Activity.RESULT_OK);
                        }

                        mProgressBarUpload.setVisibility(View.GONE);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(LOG_TAG, "Error updating artwork: " + mCurrentArtworkId, e);
                        mProgressBarUpload.setVisibility(View.GONE);
                        mSaveButton.setEnabled(true);
                        Toast.makeText(AddEditArtworkActivity.this, "Hiba a frissítés során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void addArtistToCollectionIfNeeded(String artistName) {
        if (artistName == null || artistName.trim().isEmpty()) {
            Log.d(LOG_TAG, "Artist name is empty, skipping add to 'artists' collection.");
            return;
        }
        final String artistDisplayName = artistName.trim();
        // Használjuk ugyanazt az ID logikát, mint a migrációban (ID = Eredeti név)
        final DocumentReference artistDocRef = mFirestore.collection("artists").document(artistDisplayName);

        // Létrehozzuk az adatokat (csak a displayName kell)
        Map<String, Object> artistData = new HashMap<>();
        artistData.put("displayName", artistDisplayName);
        artistData.put("nameLower", artistDisplayName.toLowerCase()); // Ha tárolni akarjuk a kisbetűset is

        artistDocRef.set(artistData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.i(LOG_TAG, "Artist '" + artistDisplayName + "' ensured in 'artists' collection."))
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error ensuring artist '" + artistDisplayName + "' in collection", e));
    }

}