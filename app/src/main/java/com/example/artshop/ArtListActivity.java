package com.example.artshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.artshop.adapter.ArtworkAdapter;
import com.example.artshop.model.ArtworkItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ArtListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ArtListActivity.class.getName();
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    // RecyclerView elemek
    private RecyclerView mRecyclerView;
    private ArrayList<ArtworkItem> mArtworkData;
    private ArtworkAdapter mAdapter;

    // Grid layout kezelése
    private int gridColumnCount = 1; // Alapból 1 oszlop (lista nézet)

    // Kosár számláló (egyelőre csak UI)
    private FrameLayout redCircle;
    private TextView countTextView;
    private int cartItems = 0;

    private Menu mMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_list);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            Log.d(LOG_TAG, "Unauthenticated user! Finishing Activity.");
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_LONG).show();
            finish(); // Vissza a MainActivity-re
            return; // Ne fusson tovább a kód itt
        } else {
            Log.d(LOG_TAG, "Authenticated user: " + user.getEmail());
        }

        // Oszlopok számának meghatározása tájolás és képernyőméret alapján
        determineGridColumnCount();

        // RecyclerView inicializálása
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridColumnCount));

        mArtworkData = new ArrayList<>();
        mAdapter = new ArtworkAdapter(this, mArtworkData);
        mRecyclerView.setAdapter(mAdapter);


        // Adatok betöltése (egyelőre statikusan)
        initializeData();

        mAdapter.updateData(new ArrayList<>(mArtworkData));
        Log.d(LOG_TAG, "Data loaded and passed to adapter. "+mAdapter.getItemCount()+" items in the adapter.");
        // ---- MÓDOSÍTÁS VÉGE ----
    }

    // Meghatározza az oszlopok számát a GridLayoutManager számára
    private void determineGridColumnCount() {
        // Alapértelmezett: 1 oszlop (mobil, portré)
        gridColumnCount = 1;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet); // values/bools.xml definiálja
        int orientation = getResources().getConfiguration().orientation;

        if (isTablet) {
            gridColumnCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 3;
        } else { // Telefon
            gridColumnCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 1 : 2;
        }
        Log.d(LOG_TAG, "Grid column count set to: " + gridColumnCount + " (isTablet: " + isTablet + ", orientation: " + orientation + ")");
    }

    // Kezeli a konfiguráció változását (pl. elforgatás)
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        determineGridColumnCount(); // Újraszámoljuk az oszlopszámot
        // Alkalmazzuk az új oszlopszámot a LayoutManager-re
        ((GridLayoutManager) mRecyclerView.getLayoutManager()).setSpanCount(gridColumnCount);
        Log.d(LOG_TAG, "Configuration changed, grid updated to " + gridColumnCount);
    }


    // Statikus adatok inicializálása (később Firebase Firestore-ból jönnek)
    private void initializeData() {
        String[] titles = getResources().getStringArray(R.array.artwork_titles);
        String[] descriptions = getResources().getStringArray(R.array.artwork_descriptions);
        String[] prices = getResources().getStringArray(R.array.artwork_prices);
        String[] artists = getResources().getStringArray(R.array.artwork_artists); // Új tömb az alkotóknak
        TypedArray images = getResources().obtainTypedArray(R.array.artwork_images);
        // Ratings could also be from an array if needed
        float[] ratings = {4.5f, 3.0f, 5.0f, 4.0f, 2.5f, 4.8f}; // Example ratings

        // Fontos: Az Activity saját listáját töltjük, nem közvetlenül az adapterét
        mArtworkData.clear(); // Töröljük a régit, ha esetleg újra hívnánk

        for (int i = 0; i < titles.length; i++) {
            // Biztonsági ellenőrzés, hogy van-e elég értékelés
            float rating = (i < ratings.length) ? ratings[i] : 3.0f; // Alapértelmezett 3.0 ha nincs elég

            mArtworkData.add(new ArtworkItem(
                    titles[i],
                    descriptions[i],
                    prices[i],
                    rating,
                    images.getResourceId(i, 0), // 0 az alapértelmezett kép, ha nincs
                    artists[i] // Alkotó hozzáadása
            ));
        }

        images.recycle(); // Fontos!
        Log.i(LOG_TAG, "initializeData finished, mArtworkData size: " + mArtworkData.size());
    }

    // --- Menü kezelése ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.art_list_menu, menu);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true);
        int color = typedValue.data;

        // Apply the color to all menu icons
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable icon = item.getIcon();
            if (icon != null) {
                icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }

        // Tint the cart icon in the custom layout
        MenuItem cartItem = menu.findItem(R.id.cart);
        if (cartItem != null) {
            View actionView = MenuItemCompat.getActionView(cartItem);
            if (actionView instanceof FrameLayout) {
                FrameLayout rootView = (FrameLayout) actionView;
                ImageView cartIcon = rootView.findViewById(R.id.cart_icon);
                if (cartIcon != null) {
                    Drawable iconDrawable = cartIcon.getDrawable();
                    if (iconDrawable != null) {
                        iconDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }


        mMenu = menu;
        MenuItem searchItem = menu.findItem(R.id.search_bar);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setMaxWidth(10000);
        searchView.setInputType(1);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Általában nem kell külön kezelni, az onQueryTextChange elég
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, "Search query changed: " + newText);
                // Alkalmazzuk a szűrőt az adapterben
                // Fontos: Ha newText üres, az adapternek vissza kell állítania a teljes listát!
                mAdapter.setShouldAnimate(newText.isEmpty());
                mAdapter.getFilter().filter(newText);
                return true; // Jelzi, hogy kezeltük az eseményt
            }


        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Elrejti a többi menüpontot, amikor a kereső kibont vagy
                setMenuItemsVisibility(false);
                Log.d(LOG_TAG, "Search view expanded, hiding other menu items.");
                return true; // Visszaadjuk, hogy engedélyezzük a kibontást
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Visszaállítjuk a többi menüpontot, amikor a kereső bezár
                setMenuItemsVisibility(true);
                Log.d(LOG_TAG, "Search view collapsed, restoring menu items.");
                // Kiürítjük a szűrőt az adapterben
                mAdapter.getFilter().filter("");
                return true; // Visszaadjuk, hogy engedélyezzük az összehúzást
            }
        });

        // Listener a kereső nézet összecsukására/kinyitására
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                // Kereső kinyílt (nem szükséges teendő itt általában)
                setMenuItemsVisibility(false);
                Log.d(LOG_TAG, "Search view expanded, hiding other menu items.");
                return true; // Visszatérés true, hogy engedélyezzük a kinyitást
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                // Ha a kereső összezárul, visszaállíthatjuk a többi menüelemet.
                setMenuItemsVisibility(true);
                // Kereső bezárult (pl. vissza gomb, 'X' ikon)
                Log.d(LOG_TAG, "Search view collapsed, clearing filter.");
                // Ürítjük a szűrőt az adapterben egy üres string átadásával
                mAdapter.getFilter().filter(""); // Ennek vissza kell állítania a teljes listát
                return true; // Visszatérés true, hogy engedélyezzük a bezárást
            }
        });

        return true;
    }

    // Segédfüggvény, amely elrejti vagy megjeleníti a többi menüelemet
    private void setMenuItemsVisibility(boolean visible) {
        // Ezek az ID-k az általad használt menüpontok
        if(mMenu != null) {
            mMenu.findItem(R.id.cart).setVisible(visible);
            mMenu.findItem(R.id.add_new_artwork).setVisible(visible);
            mMenu.findItem(R.id.log_out_button).setVisible(visible);
            // Ha vannak egyéb menüpontjaid, itt add hozzá őket.
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId(); // Használj ID-t a switch-hez

        if (itemId == R.id.log_out_button) {
            Log.d(LOG_TAG, "Logout clicked!");
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Sikeres kijelentkezés!", Toast.LENGTH_SHORT).show();
            // Visszanavigálás a MainActivity-hez
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Töröljük a back stack-et
            startActivity(intent);
            finish(); // Bezárjuk ezt az activity-t
            return true;
        } else if (itemId == R.id.cart) {
            Log.d(LOG_TAG, "Cart clicked!");
            Toast.makeText(this, "Kosár funkció (még nincs implementálva)", Toast.LENGTH_SHORT).show();
            // Ide jöhetne a kosár Activity indítása
            return true;
        } else if (itemId == R.id.add_new_artwork) {
            Log.d(LOG_TAG, "Add New Artwork clicked!");
            Toast.makeText(this, "Új mű hozzáadása (még nincs implementálva)", Toast.LENGTH_SHORT).show();
            // Ide jöhetne az új műalkotás hozzáadása Activity indítása (CRUD - Create)
            return true;
        }
        // else if (itemId == R.id.settings_button) { ... } // Beállításokhoz
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    // Kosár ikon előkészítése (ha használod a custom layoutot)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView(); // Vigyázz, null lehet, ha nincs actionLayout!

        if (rootView != null) {
            redCircle = rootView.findViewById(R.id.view_alert_red_circle);
            countTextView = rootView.findViewById(R.id.view_alert_count_textview);

            rootView.setOnClickListener(v -> onOptionsItemSelected(alertMenuItem));

        } else {
            Log.w(LOG_TAG, "Cart ActionLayout not found!");
        }


        return super.onPrepareOptionsMenu(menu);
    }

    // Kosár ikon frissítése (ezt hívja meg az adapter)
    public void updateCartIcon() {
        cartItems++; // Növeljük a számlálót (ez csak demo)
        if (countTextView == null || redCircle == null) {
            Log.w(LOG_TAG, "Cart count TextView or redCircle is null in updateCartIcon");
            return; // Kilépünk, ha a nézetek még nincsenek inicializálva
        }

        if (cartItems > 0) {
            countTextView.setText(String.valueOf(cartItems));
            redCircle.setVisibility(VISIBLE);
        } else {
            countTextView.setText("");
            redCircle.setVisibility(GONE);
        }
        Log.d(LOG_TAG, "Cart icon updated, items: " + cartItems);
    }
}