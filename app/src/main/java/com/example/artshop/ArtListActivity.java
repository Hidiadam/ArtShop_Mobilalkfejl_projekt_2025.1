package com.example.artshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.artshop.adapter.ArtworkAdapter;
import com.example.artshop.model.ArtworkItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.net.Uri; // Képfeltöltéshez kellhet
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore; // Képválasztáshoz/készítéshez
import android.view.inputmethod.EditorInfo; // SearchView bezárásához
import android.widget.ProgressBar; // Töltésjelző
import android.widget.Spinner; // Szűréshez (opcionális)
import android.widget.ArrayAdapter; // Spinner adapterhez
import android.widget.AdapterView; // Spinner listenerhez
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView; // Spinner text color
import android.util.TypedValue; // Spinner text color
import android.graphics.drawable.Drawable; // Ikon színezéshez
import androidx.core.graphics.drawable.DrawableCompat; // Ikon színezéshez
import androidx.recyclerview.widget.GridLayoutManager; // GridLayoutManager import

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Firestore lekérdezésekhez
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage; // Képfeltöltéshez
import com.google.firebase.storage.StorageReference; // Képfeltöltéshez
import com.google.firebase.auth.FirebaseAuth; // Auth
import com.google.firebase.auth.FirebaseUser; // Auth
import com.google.firebase.storage.UploadTask;

import androidx.activity.result.ActivityResultLauncher; // Képválasztáshoz/készítéshez
import androidx.activity.result.contract.ActivityResultContracts; // Képválasztáshoz/készítéshez
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider; // Képkészítéshez
import androidx.appcompat.app.AppCompatActivity; // Szükséges base class
import android.content.res.Configuration; // Konfiguráció változás figyeléshez


import java.io.File; // Képkészítéshez
import java.io.IOException; // Képkészítéshez
import java.text.SimpleDateFormat; // Képkészítéshez
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date; // Képkészítéshez
import java.util.HashMap;
import java.util.HashSet;
import java.util.List; // Fontos: List használata
import java.util.Locale; // Képkészítéshez
import java.util.Arrays; // Statikus spinner listához
import java.util.Map;
import java.util.Set;

import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import android.util.TypedValue; // Szükséges import
import android.graphics.Color; // Szükséges import
import android.graphics.drawable.Drawable; // Szükséges import
import androidx.core.graphics.drawable.DrawableCompat; // Szükséges import

import com.example.artshop.logic.CartManager;

import com.google.firebase.firestore.QuerySnapshot;



public class ArtListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ArtListActivity.class.getName();
    public static final int ADD_ARTWORK_REQUEST = 1;
    public static final int EDIT_ARTWORK_REQUEST = 2;
    private static final int NOTIFICATION_PERMISSION_CODE = 111; // Egyedi kód (Jogosultságokhoz)



    private FirebaseUser user;
    private FirebaseAuth mAuth;

    // RecyclerView
    private RecyclerView mRecyclerView;
    private List<ArtworkItem> mArtworkData; // List<ArtworkItem> használata
    private ArtworkAdapter mAdapter;
    private int gridColumnCount = 1;

    // Firestore
    private FirebaseFirestore mFirestore;
    private CollectionReference mArtworkCollection;

    // UI Elemek
    private FrameLayout redCircle;
    private TextView countTextView;
    private ProgressBar mProgressBar;
    private TextView mEmptyListTextView;
    private Menu mMenu;
    private SearchView mSearchView = null;

    // Kosár (egyelőre csak UI)
    private int cartItems = 0;

    // Háttérszolgáltatások és Jogosultságok
    private NotificationHelper mNotificationHelper;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;
    private static final int JOB_ID = 0;

    // Lekérdezési paraméterek
    private String currentFilterArtist = null;
    private String currentSortField = "createdAt";
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;
    private FrameLayout cartRootView = null;

    private static final int PAGE_SIZE = 5;
    private DocumentSnapshot lastVisible = null;
    private boolean isLoadingMore = false;

    private String currentSearchText = null;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Query.Direction DEFAULT_SORT_DIRECTION = Query.Direction.DESCENDING;

    private final android.os.Handler spinnerHandler = new android.os.Handler(Looper.getMainLooper());


    // --- ActivityResult Launcherek ---
    // Új elem hozzáadása/szerkesztése
    private final ActivityResultLauncher<Intent> artworkFormLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(LOG_TAG, "Artwork form finished, RESULT_OK. Refreshing data.");
                    queryData(); // Frissítjük a listát, ha sikeres volt a mentés/módosítás
                    Toast.makeText(this, "Műalkotás sikeresen mentve!", Toast.LENGTH_SHORT).show();

                    Intent data = result.getData();
                    if (data != null && data.hasExtra("CHECK_ARTIST_DELETION")) {
                        String oldArtistToCheck = data.getStringExtra("CHECK_ARTIST_DELETION");
                        if (oldArtistToCheck != null && !oldArtistToCheck.isEmpty()) {
                            Log.d(LOG_TAG, "Received request to check old artist for deletion: " + oldArtistToCheck);
                            checkAndDeleteArtistIfNeeded(oldArtistToCheck);
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "Artwork form finished, but not RESULT_OK (Code: " + result.getResultCode() + ").");
                }
            });

    // Launcher az értesítési engedélykéréshez (API 33+)
    private final ActivityResultLauncher<String> requestPermissionLauncherNotification =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i(LOG_TAG, "Notification permission granted by user.");
                    // Itt akár küldhetnénk egy teszt értesítést, vagy csak tudomásul vesszük
                    mNotificationHelper.send("Köszönjük az engedélyt!");
                } else {
                    Log.w(LOG_TAG, "Notification permission denied by user.");
                    Toast.makeText(this, "Értesítési engedély elutasítva.", Toast.LENGTH_SHORT).show();
                    // Itt lehetne egy dialógust megjeleníteni, ami elmagyarázza, miért kell az engedély.
                }
            });


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) // JobScheduler miatt
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_list);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null) {
            Log.d(LOG_TAG, "Unauthenticated user! Finishing Activity.");
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(LOG_TAG, "Authenticated user: " + user.getEmail());

        // UI Elemek inicializálása
        mProgressBar = findViewById(R.id.progressBar);
        mEmptyListTextView = findViewById(R.id.textViewEmptyList);
        mRecyclerView = findViewById(R.id.recyclerView);

        determineGridColumnCount();
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridColumnCount));
        mArtworkData = new ArrayList<>();
        mAdapter = new ArtworkAdapter(this, mArtworkData);
        mRecyclerView.setAdapter(mAdapter);

        // Firestore inicializálása
        mFirestore = FirebaseFirestore.getInstance();
        mArtworkCollection = mFirestore.collection("artworks"); // Kollekció neve

        // Háttérszolgáltatások inicializálása
        mNotificationHelper = new NotificationHelper(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        // Kezdeti adatlekérdezés (onStart helyett itt, hogy a ProgressBar látszódjon)
        mProgressBar.setVisibility(View.VISIBLE); // Töltésjelző mutatása
        queryData();

        // JobScheduler indítása (csak példa, lehet feltételhez kötni)
        scheduleJob();

        // Jogosultság kérése értesítésekhez (API 33+)
        requestNotificationPermissionIfNeeded();

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int lastPos   = lm.findLastVisibleItemPosition();
                int totalItem = mAdapter.getItemCount();

                // ha az utolsó 2 elem valamelyike látszik → oldal betöltése
                if (!isLoadingMore && lastVisible != null && lastPos >= totalItem - 2) {
                    Log.d(LOG_TAG, "→ trigger loadNextPage()");
                    loadNextPage();
                }
            }
        });

        Log.i(LOG_TAG, "onCreate finished.");
    }


    private static String stripAccents(String text) {
        if (text == null) return "";
        return java.text.Normalizer
                .normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }


    // Adatok lekérdezése Firestore-ból
    private void queryData() {
        mArtworkData.clear();
        mAdapter.notifyDataSetChanged();
        mProgressBar.setVisibility(View.VISIBLE); // Mutassuk a töltést
        mEmptyListTextView.setVisibility(View.GONE); // Rejtsük az üres lista üzenetet
        lastVisible = null; // Reset pagination for a fresh query
        isLoadingMore = false;
        mAdapter.setShouldAnimate(true);

        Query query;

        // --- SEARCH LOGIC ---
        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            Log.d(LOG_TAG, "Applying SEARCH filter: titleLower starts with '" + currentSearchText + "'");
            query = mArtworkCollection
                    .orderBy("titleLower") // Must order by the field we query range on
                    .startAt(currentSearchText)
                    .endAt(currentSearchText + "\uf8ff")
                    .limit(PAGE_SIZE); // Apply pagination limit to search results too
            // Reset artist filter when searching title
            currentFilterArtist = null;
        }
        // --- REGULAR FILTER/SORT LOGIC ---
        else {
            Log.d(LOG_TAG, "Applying REGULAR filter/sort. Artist: " + currentFilterArtist + ", Sort: " + currentSortField + " " + currentSortDirection);
            query = mArtworkCollection.orderBy(currentSortField, currentSortDirection);

            // Apply artist filter if selected
            if (currentFilterArtist != null) { // Removed checks for empty/ "Minden alkotó" as they should result in null
                query = query.whereEqualTo("artist", currentFilterArtist);
            }
            query = query.limit(PAGE_SIZE);
        }


        // --- Execute the constructed query ---
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<ArtworkItem> newItems = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                ArtworkItem item = document.toObject(ArtworkItem.class);
                item.setId(document.getId());
                newItems.add(item);
            }

            if (!newItems.isEmpty()) {
                mArtworkData.addAll(newItems);
                Log.d(LOG_TAG, "queryData fetched " + newItems.size() + " items. Total now: " + mArtworkData.size());
            } else {
                Log.d(LOG_TAG, "queryData fetched 0 items for current query/page.");
            }

            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
            lastVisible = docs.isEmpty() ? null : docs.get(docs.size() - 1);
            isLoadingMore = false;
            mAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
            showEmptyListMessage(mArtworkData.isEmpty());

            Log.d(LOG_TAG, "queryData() snapshot size=" + queryDocumentSnapshots.size() +
                    ", lastVisible set to " + (lastVisible != null) + ", List empty: " + mArtworkData.isEmpty());

            // If searching and results are empty, show a specific message maybe
            if (currentSearchText != null && !currentSearchText.isEmpty() && mArtworkData.isEmpty()) {
                mEmptyListTextView.setText("Nincs találat a keresésre: '" + currentSearchText + "'");
            } else if (mArtworkData.isEmpty()) {
                mEmptyListTextView.setText(R.string.empty_list_message); // Default empty message
            }
        }).addOnFailureListener(e -> {
            Log.e(LOG_TAG, "Error querying Firestore documents: ", e);
            mProgressBar.setVisibility(View.GONE);
            mArtworkData.clear();
            mAdapter.notifyDataSetChanged();
            isLoadingMore = false;
            lastVisible = null;
            showEmptyListMessage(true); // Hiba esetén is üresnek tekintjük
            Toast.makeText(ArtListActivity.this, "Hiba a műalkotások lekérdezésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Kezeld az INDEX hibát: Ha az Exception üzenete tartalmazza, hogy index szükséges
            if (e.getMessage() != null && e.getMessage().contains("index")) {
                Log.w(LOG_TAG, "Firestore index might be missing. Check Firestore console or logs for index creation link.");
                Toast.makeText(this, "A lekérdezéshez index szükséges a Firestore-ban. Ellenőrizd a konzolt!", Toast.LENGTH_LONG).show();
            }
        });
    }


    // CRUD - Delete metódus (Adapterből hívva)
    public void deleteArtwork(ArtworkItem item) {
        if (item == null || item.getId() == null) {
            Log.e(LOG_TAG, "Cannot delete item with null ID.");
            Toast.makeText(this, "Hiba: Nem törölhető elem azonosító nélkül.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String artworkIdToDelete = item.getId();
        final String artistName = item.getArtist();
        final String artworkTitle = item.getTitle();
        final String imageUrl = item.getImageUrl();


        Log.d(LOG_TAG, "Attempting to delete item with ID: " + item.getId());

        // Törlés Firestore-ból
        mArtworkCollection.document(item.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(LOG_TAG, "Artwork successfully deleted: " + item.getId());
                    Toast.makeText(this, "'" + item.getTitle() + "' törölve.", Toast.LENGTH_SHORT).show();

                    checkAndDeleteArtistIfNeeded(artistName);

                    deleteOrMoveStorageImage(imageUrl);

                    removeItemFromLocalList(artworkIdToDelete);
                    updateCartIconCount(CartManager.getInstance().getCartItemCount());
                    queryData();

                    mNotificationHelper.send("'" + artworkTitle + "' törölve."); // Példa értesítés küldésre
                    invalidateOptionsMenu();

                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error deleting artwork: " + item.getId(), e);
                    Toast.makeText(this, "Hiba a törlés során: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void removeItemFromLocalList(String artworkId) {
        if (artworkId == null) return;
        ArtworkItem itemToRemove = null;
        for(ArtworkItem currentItem : mArtworkData) {
            if(artworkId.equals(currentItem.getId())) {
                itemToRemove = currentItem;
                break;
            }
        }
        if (itemToRemove != null) {
            int position = mArtworkData.indexOf(itemToRemove);
            mArtworkData.remove(itemToRemove);
            mAdapter.notifyItemRemoved(position);
            Log.d(LOG_TAG, "Removed item from local list: " + artworkId);
        }
    }

    private void deleteArtistFromCollection(String artistDisplayName) {
        // Használjuk ugyanazt az ID-t, mint a migrációban/hozzáadáskor (eredeti név)
        mFirestore.collection("artists").document(artistDisplayName).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.i(LOG_TAG, "Successfully deleted artist '" + artistDisplayName + "' from 'artists' collection.");
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting artist '" + artistDisplayName + "' from 'artists' collection", e));
    }


    private void checkAndDeleteArtistIfNeeded(String artistName) {
        if (artistName == null || artistName.trim().isEmpty()) {
            Log.d(LOG_TAG, "Artist name is empty, no need to check 'artists' collection.");
            return;
        }
        final String artistDisplayName = artistName.trim();
        Log.d(LOG_TAG, "Checking if artist '" + artistDisplayName + "' has other artworks...");

        // Lekérdezzük, van-e MÁSIK műalkotása ennek az alkotónak
        mArtworkCollection.whereEqualTo("artist", artistDisplayName)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot == null || snapshot.isEmpty()) {
                            // Nem találtunk másik műalkotást ettől az alkotótól
                            Log.i(LOG_TAG, "Artist '" + artistDisplayName + "' has no other artworks. Deleting from 'artists' collection.");
                            deleteArtistFromCollection(artistDisplayName);
                        } else {
                            // Találtunk még legalább egyet, nem töröljük az alkotót
                            Log.d(LOG_TAG, "Artist '" + artistDisplayName + "' still has other artworks. Not deleting from 'artists' collection.");
                        }
                    } else {
                        // Hiba történt a lekérdezéskor
                        Log.e(LOG_TAG, "Error checking for other artworks by artist '" + artistDisplayName + "'", task.getException());
                        // Ebben az esetben óvatosságból nem töröljük az alkotót
                    }
                });
    }


    private void deleteOrMoveStorageImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

                // Döntés: Végleges törlés VAGY Áthelyezés?
                boolean moveToDeletedFolder = true; // Állítsd false-ra a végleges törléshez

                if (moveToDeletedFolder) {
                    // ÁTHELYEZÉS (másolás + régi törlése)
                    StorageReference deletedRef = FirebaseStorage.getInstance()
                            .getReference()
                            .child("deleted") // Győződj meg róla, hogy ez a mappa létezik vagy kezeli a rendszer
                            .child(imageRef.getName());

                    // Alternatíva nagy fájlokhoz: Stream-elés (bonyolultabb hibakezelés)
                    imageRef.getStream((state, inputStream) -> {
                        UploadTask uploadTask = deletedRef.putStream(inputStream);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            Log.d(LOG_TAG, "Image successfully moved to 'deleted' folder: " + deletedRef.getPath());
                            imageRef.delete()
                                    .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Original image deleted from Storage after move: " + imageUrl))
                                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting original image after move: " + imageUrl, e));
                        }).addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to upload image stream to 'deleted' folder.", e));
                    }).addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to get image stream for moving: " + imageUrl, e));

                } else {
                    // VÉGLEGES TÖRLÉS
                    imageRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Associated image permanently deleted from Storage: " + imageUrl))
                            .addOnFailureListener(e -> Log.e(LOG_TAG, "Error permanently deleting image from Storage: " + imageUrl, e));
                }

            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "Invalid image URL, cannot process Storage image: " + imageUrl, e);
            }
        } else {
            Log.d(LOG_TAG, "No image URL provided, skipping Storage operation.");
        }
    }

    // Visszaadja a témából kinyert színt (ha nem található, szürke)
    private int resolveThemeColor(int attrId) {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(attrId, tv, true)) {
            if (tv.resourceId != 0) {
                return ContextCompat.getColor(this, tv.resourceId);
            }
            return tv.data;
        }
        return Color.GRAY;
    }

    // CRUD - Update metódus (Adapterből hívva) - Activity indítása
    public void editArtwork(String artworkId) {
        if (artworkId == null) {
            Log.e(LOG_TAG, "Cannot edit item with null ID.");
            Toast.makeText(this, "Hiba: Nem szerkeszthető elem azonosító nélkül.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(LOG_TAG, "Starting AddEditArtworkActivity for editing ID: " + artworkId);
        Intent intent = new Intent(this, AddEditArtworkActivity.class);
        intent.putExtra(AddEditArtworkActivity.EXTRA_ARTWORK_ID, artworkId);
        artworkFormLauncher.launch(intent);
    }

    // Metódus az üres lista üzenet megjelenítésére/elrejtésére
    public void showEmptyListMessage(boolean show) {
        if (mEmptyListTextView != null) {
            mEmptyListTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            Log.d(LOG_TAG, "Empty list message visibility set to: " + show);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void loadNextPage() {

        if (lastVisible == null || isLoadingMore) {
            Log.d(LOG_TAG, "loadNextPage skipped: lastVisible=" + (lastVisible != null) + ", isLoadingMore=" + isLoadingMore);
            return;
        }
        isLoadingMore = true;
        Log.d(LOG_TAG, "loadNextPage() starting after document ID: " + lastVisible.getId());

        Query next;

        // --- SEARCH PAGINATION ---
        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            Log.d(LOG_TAG, "Paginating SEARCH results for: '" + currentSearchText + "'");
            next = mArtworkCollection
                    .orderBy("titleLower")
                    .startAt(currentSearchText)
                    .endAt(currentSearchText + "\uf8ff")
                    .startAfter(lastVisible)
                    .limit(PAGE_SIZE);
        }
        // --- REGULAR PAGINATION ---
        else {
            Log.d(LOG_TAG, "Paginating REGULAR results. Artist: " + currentFilterArtist + ", Sort: " + currentSortField);
            next = mArtworkCollection
                    .orderBy(currentSortField, currentSortDirection)
                    .startAfter(lastVisible)
                    .limit(PAGE_SIZE);
            // Apply artist filter if active
            if (currentFilterArtist != null) {
                next = next.whereEqualTo("artist", currentFilterArtist);
            }
        }


        next.get().addOnSuccessListener(snap -> {
            int start = mArtworkData.size();
            List<ArtworkItem> newItems = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                ArtworkItem item = doc.toObject(ArtworkItem.class);
                item.setId(doc.getId());
                newItems.add(item);
            }
            if (!newItems.isEmpty()) {
                mArtworkData.addAll(newItems);

                mAdapter.notifyItemRangeInserted(start, snap.size()); // animált beszúrás
                Log.d(LOG_TAG, "loadNextPage() success. Added " + newItems.size() + " items. New total: " + mArtworkData.size());
                List<DocumentSnapshot> docs = snap.getDocuments();
                lastVisible = docs.isEmpty() ? null : docs.get(docs.size() - 1);
                Log.d(LOG_TAG, "loadNextPage() new lastVisible set: " + (lastVisible != null));
            }else {
                Log.d(LOG_TAG, "loadNextPage() success, but no more items found.");
                lastVisible = null;
            }
            isLoadingMore = false; // Reset loading flag
        }).addOnFailureListener(e -> {
            Log.e(LOG_TAG, "Paging query failed", e);
            isLoadingMore = false;
            Toast.makeText(ArtListActivity.this, "Hiba a további elemek betöltésekor.", Toast.LENGTH_SHORT).show();
        });
    }

    // ÚJ Metódus: Értesítési jogosultság kérése, ha szükséges
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Notification permission already granted.");
                // Engedély már megvan
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.w(LOG_TAG, "Showing rationale for notification permission.");
                new AlertDialog.Builder(this)
                        .setTitle("Értesítési Engedély")
                        .setMessage("Az alkalmazás szeretne értesítéseket küldeni az új műalkotásokról és akciókról. Kérjük, engedélyezd!")
                        .setPositiveButton("Engedély Kérése", (dialog, which) -> {
                            requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS);
                        })
                        .setNegativeButton("Elutasít", null)
                        .show();
            }
            else {
                // Első alkalommal kérjük az engedélyt
                Log.i(LOG_TAG, "Requesting notification permission for the first time.");
                requestPermissionLauncherNotification.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // API 33 alatt nincs szükség explicit engedélyre
            Log.d(LOG_TAG, "Notification permission not required (API < 33).");
        }
    }


    // --- Menü Kezelése ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.art_list_menu, menu);
        mMenu = menu;

        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        if (alertMenuItem != null) {
            alertMenuItem.setActionView(R.layout.custom_menu_item);
            cartRootView = (FrameLayout) alertMenuItem.getActionView();
            if (cartRootView != null) {
                redCircle = cartRootView.findViewById(R.id.view_alert_red_circle);
                countTextView = cartRootView.findViewById(R.id.view_alert_count_textview);
                cartRootView.setOnClickListener(v -> onOptionsItemSelected(alertMenuItem));
            }
            // Initial update (optional, onPrepare will do it too)
            updateCartIconCount(CartManager.getInstance().getCartItemCount());
        }

        // Menü ikonok színezése (ha szükséges)
        tintMenuIcons(menu);

        // Kereső beállítása
        MenuItem searchItem = menu.findItem(R.id.search_bar);
        mSearchView = (SearchView) searchItem.getActionView(); // SearchView referencia mentése
        setupSearchView(searchItem);

        // Szűrő/Rendező Spinner beállítása (Példa: Előadók szerinti szűrés)
        spinnerHandler.postDelayed(() -> {
            if (mMenu != null) {
                Log.d(LOG_TAG, "Delayed spinner refresh from onCreaeOptionsMenu()");
                setupFilterSpinner(mMenu);
            }
        }, 500);
        updateCartIconCount(CartManager.getInstance().getCartItemCount());
        return true;
    }

    private void setupSearchView(MenuItem searchItem) {
        if (mSearchView == null) return;

        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Trigger search when user presses search button on keyboard
                Log.d(LOG_TAG, "Search submitted: " + query);
                currentSearchText = query.toLowerCase(Locale.ROOT).trim();
                lastVisible = null;
                queryData();
                mSearchView.clearFocus(); // Hide keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                String trimmedText = newText.trim();
                if (trimmedText.isEmpty() && currentSearchText != null) {
                    Log.d(LOG_TAG, "Search text cleared in change listener.");
                    currentSearchText = null; // Clear the search term
                    lastVisible = null; // Reset pagination
                    currentSortField = DEFAULT_SORT_FIELD;
                    currentSortDirection = DEFAULT_SORT_DIRECTION;
                    currentFilterArtist = null; // Clear artist filter too if search overrides it
                    queryData(); // Refresh data
                }
                return true;
            }
        });

        // Listener for the 'X' close button inside the SearchView
        mSearchView.setOnCloseListener(() -> {
            Log.d(LOG_TAG, "Search view closed via 'X' button.");
            if (currentSearchText != null) {
                currentSearchText = null;
                lastVisible = null;
                currentSortField = DEFAULT_SORT_FIELD;
                currentSortDirection = DEFAULT_SORT_DIRECTION;
                currentFilterArtist = null;
                queryData();
            }
            return false;
        });

        // Modify the expand/collapse listener to handle search term clearing
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                setMenuItemsVisibility(mMenu, item.getItemId(), false);
                Log.d(LOG_TAG, "Search view expanded.");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                setMenuItemsVisibility(mMenu, item.getItemId(), true);
                Log.d(LOG_TAG, "Search view collapsed.");
                // Clear search term and requery if a search was active
                if (currentSearchText != null) {
                    Log.d(LOG_TAG, "Clearing search term on collapse.");
                    currentSearchText = null;
                    lastVisible = null;
                    // Optionally restore previous sort/filter or use defaults
                    currentSortField = DEFAULT_SORT_FIELD;
                    currentSortDirection = DEFAULT_SORT_DIRECTION;
                    currentFilterArtist = null;
                    queryData();
                }
                invalidateOptionsMenu();
                return true;
            }
        });
    }

    // ÚJ: Spinner beállítása a szűréshez/rendezéshez
    private void setupFilterSpinner(Menu menu) {
        MenuItem filterItem = menu.findItem(R.id.filter_spinner_item);
        Spinner spinner = (Spinner) filterItem.getActionView();

        filterItem.setIcon(R.drawable.baseline_filter_list_24);

        Drawable rawIcon = filterItem.getIcon();
        if (rawIcon != null) {
            Drawable wrapped = DrawableCompat.wrap(rawIcon.mutate());
            DrawableCompat.setTint(wrapped, resolveThemeColor(android.R.attr.colorControlNormal));
            filterItem.setIcon(wrapped);
        }
        final int dropdownTextColor = resolveThemeColor(android.R.attr.textColorPrimary);

        // Create a custom adapter that shows only icon in the toolbar
        List<String> artists = new ArrayList<>();
        artists.add("Minden alkotó"); // First option to clear filter

        // Create a custom adapter that shows only the icon in the closed state
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, artists) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView = new ImageView(getContext());
                imageView.setImageResource(R.drawable.baseline_filter_list_24);
                imageView.setColorFilter(resolveThemeColor(android.R.attr.colorControlNormal), PorterDuff.Mode.SRC_IN);
                return imageView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(dropdownTextColor);
                return tv;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        loadArtistsForSpinner(artists, spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedArtist = (String) parent.getItemAtPosition(position);
                Log.d(LOG_TAG, "Filter Spinner selected: " + selectedArtist);

                String newFilter = null;
                if (!selectedArtist.equals("Minden alkotó")) {
                    newFilter = selectedArtist;
                }

                // Check if filter actually changed
                if ((currentFilterArtist == null && newFilter != null) ||
                        (currentFilterArtist != null && !currentFilterArtist.equals(newFilter))) {

                    Log.d(LOG_TAG, "Filter Spinner changed to: " + selectedArtist);
                    currentFilterArtist = newFilter;

                // Close search if open
                if (mSearchView != null && !mSearchView.isIconified()) {
                    mSearchView.setIconified(true);
                    mSearchView.onActionViewCollapsed();
                }

                    lastVisible = null;
                    isLoadingMore = false;
                    queryData();
                } else {
                    Log.d(LOG_TAG, "Filter Spinner selection didn't change the actual filter.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilterArtist = null;
                queryData();
            }
        });
    }

    // New method to load artists directly from Firestore
    private void loadArtistsForSpinner(List<String> artistsDisplayList, Spinner spinner) {
        Log.d(LOG_TAG, "Loading artists for spinner from 'artists' collection by nameLower...");
        final CollectionReference artistsRef = mFirestore.collection("artists");
        artistsRef.orderBy("nameLower", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Töröljük az előzőleg betöltött neveket (kivéve a "Minden alkotó"-t)
                    if (artistsDisplayList.size() > 1) {
                        artistsDisplayList.subList(1, artistsDisplayList.size()).clear();
                    }

                    int count = 0;
                    // Hozzáadjuk az új neveket a listához
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // A MEGJELENÍTENDŐ nevet olvassuk ki
                        String displayName = document.getString("displayName");
                        if (displayName != null && !displayName.isEmpty()) {
                            artistsDisplayList.add(displayName); // A listába az eredeti név kerül
                            count++;
                        } else {
                            Log.w(LOG_TAG, "Artist document found with missing/empty displayName. ID: " + document.getId());
                        }
                    }

                    // Értesítjük az adaptert
                    if (spinner.getAdapter() instanceof ArrayAdapter) {
                        ((ArrayAdapter<?>) spinner.getAdapter()).notifyDataSetChanged();
                    }

                    Log.i(LOG_TAG, "Loaded " + count + " artists from 'artists' collection for spinner.");

                }).addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error loading artists from 'artists' collection for spinner", e);
                    Toast.makeText(this, "Hiba az alkotók listájának betöltésekor.", Toast.LENGTH_SHORT).show();
                    // Fallback logika...
                    if (artistsDisplayList.size() <= 1) {
                        artistsDisplayList.addAll(Arrays.asList("Hiba", "Fallback1", "Fallback2"));
                        if (spinner.getAdapter() instanceof ArrayAdapter) {
                            ((ArrayAdapter<?>) spinner.getAdapter()).notifyDataSetChanged();
                        }
                    }
                });
    }


    // Menüpontok láthatóságának beállítása
    private void setMenuItemsVisibility(Menu menu, int ignoreItemId, boolean visible) {
        if (menu == null) {
            Log.w(LOG_TAG, "setMenuItemsVisibility called with null menu");
            return;
        }
        Log.d(LOG_TAG, "Setting menu items visibility to " + visible + ", ignoring item ID: " + ignoreItemId);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            if (item.getItemId() != ignoreItemId) {
                item.setVisible(visible);
                Log.v(LOG_TAG, "Item '" + item.getTitle() + "' visibility set to " + visible);
            } else {
                Log.v(LOG_TAG, "Ignoring item '" + item.getTitle() + "' in visibility toggle.");
            }
        }
    }

    // Menü ikonok színezése (ha a téma nem kezeli jól)
    private void tintMenuIcons(Menu menu) {
        if (menu == null) return;

        int defaultColor = Color.GRAY;
        int iconColor = defaultColor;

        try {
            TypedValue typedValue = new TypedValue();
            boolean resolved = getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true);

            if (resolved) {
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    iconColor = typedValue.data;
                } else if (typedValue.resourceId != 0) {
                    // Érvényes erőforrás ID, töltsük be a színt
                    iconColor = ContextCompat.getColor(this, typedValue.resourceId);
                } else {
                    Log.w(LOG_TAG, "Theme attribute colorControlNormal resolved but resourceId is 0. Using default color.");
                    iconColor = defaultColor;
                }
            } else {
                Log.e(LOG_TAG, "Failed to resolve theme attribute colorControlNormal. Using default color.");
                iconColor = defaultColor;
            }

            // Alkalmazzuk a színt az ikonokra
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                Drawable icon = item.getIcon();
                if (icon != null) {
                    Drawable wrappedIcon = DrawableCompat.wrap(icon.mutate());
                    DrawableCompat.setTint(wrappedIcon, iconColor);
                    item.setIcon(wrappedIcon);
                }
            }

            // Kosár ikon színezése (ha custom layout van) - ugyanezzel a logikával
            MenuItem cartItem = menu.findItem(R.id.cart);
            if (cartItem != null && cartItem.getActionView() != null) {
                ImageView cartIconView = cartItem.getActionView().findViewById(R.id.cart_icon);
                if (cartIconView != null && cartIconView.getDrawable() != null) {
                    Drawable iconDrawable = cartIconView.getDrawable();
                    Drawable wrappedIcon = DrawableCompat.wrap(iconDrawable.mutate());
                    DrawableCompat.setTint(wrappedIcon, iconColor);
                    cartIconView.setImageDrawable(wrappedIcon);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected error during tinting menu icons", e);
        }
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        boolean queryNeeded = false;

        if (itemId == R.id.log_out_button) {
            handleLogout();
            return true;
        } else if (itemId == R.id.cart) {
            handleCartClick();
            return true;
        } else if (itemId == R.id.add_new_artwork) {
            handleAddNewArtwork();
            return true;
        } else if (itemId == R.id.sort_by_price_asc) {
            if (!currentSortField.equals("price") || currentSortDirection != Query.Direction.ASCENDING) {
                currentSortField = "price";
                currentSortDirection = Query.Direction.ASCENDING;
                queryNeeded = true;
                Toast.makeText(this, "Rendezés: Ár szerint növekvő", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.sort_by_price_desc) {
            if (!currentSortField.equals("price") || currentSortDirection != Query.Direction.DESCENDING) {
                currentSortField = "price";
                currentSortDirection = Query.Direction.DESCENDING;
                queryNeeded = true;
                Toast.makeText(this, "Rendezés: Ár szerint csökkenő", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.sort_by_artist) {
            if (!currentSortField.equals("artistLower") || currentSortDirection != Query.Direction.ASCENDING) {
                currentSortField = "artistLower"; // Use the lowercase field
                currentSortDirection = Query.Direction.ASCENDING;
                queryNeeded = true;
                Toast.makeText(this, "Rendezés: Alkotó szerint", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.sort_by_date) {
            if (!currentSortField.equals("createdAt") || currentSortDirection != Query.Direction.DESCENDING) {
                currentSortField = "createdAt";
                currentSortDirection = Query.Direction.DESCENDING;
                queryNeeded = true;
                Toast.makeText(this, "Rendezés: Dátum szerint (legújabb elöl)", Toast.LENGTH_SHORT).show();
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        // Call queryData only if sort/filter changed
        if (queryNeeded) {
            lastVisible = null;
            isLoadingMore = false;
            queryData();
        }
        return true;
    }

    private void handleLogout() {
        Log.d(LOG_TAG, "Logout clicked!");
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Sikeres kijelentkezés!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleCartClick() {
        Log.d(LOG_TAG, "Cart menu item clicked!");
        Intent intent = new Intent(this, CartActivity.class);
        startActivity(intent);
    }

    private void handleAddNewArtwork() {
        Log.d(LOG_TAG, "Add New Artwork clicked!");
        Intent intent = new Intent(this, AddEditArtworkActivity.class);
        // Nem adunk át ID-t, így az AddEditArtworkActivity tudni fogja, hogy új elemről van szó
        artworkFormLauncher.launch(intent);
    }


    // Kosár ikon előkészítése és frissítése (nagyrészt változatlan)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onPrepareOptionsMenu called.");
        updateCartIconCount(CartManager.getInstance().getCartItemCount());
        return super.onPrepareOptionsMenu(menu);
    }

    // Belső metódus a UI frissítésére
    public void updateCartIconCount(int count) {
        runOnUiThread(() -> {                  // UI thread safety
            cartItems = count;
            if (redCircle == null || countTextView == null) return;

            if (cartItems > 0) {
                countTextView.setText(String.valueOf(cartItems));
                redCircle.setVisibility(View.VISIBLE);
            } else {
                countTextView.setText("");
                redCircle.setVisibility(View.GONE);
            }
        });
        Log.d(LOG_TAG, "Cart icon UI updated with count: " + cartItems);
    }

    // --- Lifecycle és Háttérszolgáltatások ---

    // Példa Lifecycle Hook használatra (nem onCreate)
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "onStart - Activity becomes visible.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume");
        updateCartIconCount(CartManager.getInstance().getCartItemCount());

        if (mSearchView != null) {
            mSearchView.clearFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        mSearchView.getWindowToken(),
                        0
                );
            }
        }
        spinnerHandler.postDelayed(() -> {
            if (mMenu != null) {
                Log.d(LOG_TAG, "Delayed spinner refresh from onResume()");
                setupFilterSpinner(mMenu);
            }
        }, 500);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
    }

    // Háttérszolgáltatás - JobScheduler beállítása
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) // API 21+ szükséges
    private void scheduleJob() {
        ComponentName serviceName = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceName)
                // Feltételek (pl. csak töltés közben, csak wifin)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .setPeriodic(AlarmManager.INTERVAL_FIFTEEN_MINUTES); // Példa: 15 percenként ismétlődjön (minimum 15 perc)

        JobInfo jobInfo = builder.build();

        // Ellenőrizzük, hogy a JobScheduler null-e (pl. emulátoron, ami nem támogatja)
        if (mJobScheduler == null) {
            Log.e(LOG_TAG, "JobScheduler is null, cannot schedule job.");
            return;
        }

        int resultCode = mJobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(LOG_TAG, "Job scheduled successfully! ID: " + JOB_ID);
        } else {
            Log.e(LOG_TAG, "Job scheduling failed! Code: " + resultCode);
            Toast.makeText(this, "Háttér értesítés ütemezése sikertelen.", Toast.LENGTH_SHORT).show();
        }
    }

    // Elforgatás kezelése (már implementálva volt, csak ellenőrzés)
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        determineGridColumnCount();
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            ((GridLayoutManager) mRecyclerView.getLayoutManager()).setSpanCount(gridColumnCount);
            Log.d(LOG_TAG, "Configuration changed, grid updated to " + gridColumnCount);
        }
    }

    private void determineGridColumnCount() {
        gridColumnCount = 1;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        int orientation = getResources().getConfiguration().orientation;
        if (isTablet) {
            gridColumnCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 3;
        } else {
            gridColumnCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        }
        Log.d(LOG_TAG, "Grid column count set to: " + gridColumnCount);
    }

    // Háttérszolgáltatás - AlarmManager (Alternatíva JobScheduler helyett, API < 21 esetén hasznos)
    /*
    private void setAlarmManager() {
        long repeatInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES; // 15 percenként
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;

        Intent intent = new Intent(this, AlarmReceiver.class); // Kell egy AlarmReceiver BroadcastReceiver
        // FLAG_IMMUTABLE vagy FLAG_MUTABLE megadása kötelező API 31+ esetén
        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, JOB_ID, intent, flags);

        // Ébresztéshez pontos időzítés (energiatakarékosabb lehet az inexact)
        // mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, repeatInterval, pendingIntent);

        // Kevésbé pontos, de energiatakarékosabb
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, repeatInterval, pendingIntent);

        Log.d(LOG_TAG, "AlarmManager set to repeat every " + repeatInterval / 60000 + " minutes.");

        // Leállítás: mAlarmManager.cancel(pendingIntent);
    }
    */
}