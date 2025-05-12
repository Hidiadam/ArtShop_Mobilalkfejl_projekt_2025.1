package com.example.artshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.artshop.adapter.CartAdapter;
import com.example.artshop.logic.CartManager;
import com.example.artshop.model.ArtworkItem;

import java.util.List;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private static final String LOG_TAG = CartActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private CartAdapter mAdapter;
    private List<ArtworkItem> mCartItems;
    private TextView mEmptyCartTextView;
    private Button mCheckoutButton;
    private TextView mTotalPriceTextView;
    private TextView mTotalLabelTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        setTitle("Kosár"); // Activity cím beállítása

        // UI Elemek
        mRecyclerView = findViewById(R.id.recyclerViewCart);
        mEmptyCartTextView = findViewById(R.id.textViewEmptyCart);
        mCheckoutButton = findViewById(R.id.buttonCheckout);
        mTotalPriceTextView = findViewById(R.id.textViewTotalPrice);
        mTotalLabelTextView = findViewById(R.id.textViewTotalLabel);

        // Adatok lekérése a CartManagerből
        mCartItems = CartManager.getInstance().getCartItems();

        // Adapter és RecyclerView beállítása
        mAdapter = new CartAdapter(this, mCartItems);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // Eseménykezelő a "Megrendelés" gombra
        mCheckoutButton.setOnClickListener(v -> checkout());

        // Kezdeti állapot ellenőrzése (üres kosár?)
        checkIfCartIsEmpty();
        // updateTotalPrice();
        Log.d(LOG_TAG, "CartActivity created. Items in cart: " + mCartItems.size());
    }

    // Kosár lista frissítése (az Adapter hívja meg, miután eltávolított egy elemet)
    public void refreshCartList() {
        Log.d(LOG_TAG, "Refreshing cart list.");
        mCartItems = CartManager.getInstance().getCartItems(); // Friss lista lekérése
        mAdapter.updateCartItems(mCartItems); // Adapter értesítése az új listáról
        checkIfCartIsEmpty();
        // updateTotalPrice(); // Teljes ár frissítése
    }

    // Ellenőrzi, hogy a kosár üres-e, és beállítja a nézetek láthatóságát
    private void checkIfCartIsEmpty() {
        if (mCartItems.isEmpty()) {
            mEmptyCartTextView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mCheckoutButton.setEnabled(false); // Ne lehessen üres kosárral fizetni
            mTotalLabelTextView.setVisibility(View.GONE);
            mTotalPriceTextView.setVisibility(View.GONE);
            Log.d(LOG_TAG, "Cart is empty.");
        } else {
            mEmptyCartTextView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mCheckoutButton.setEnabled(true);
            mTotalLabelTextView.setVisibility(View.VISIBLE);
            mTotalPriceTextView.setVisibility(View.VISIBLE);
            updateTotalPrice(); // HOZZÁADVA: Ár frissítése csak akkor, ha nem üres
            Log.d(LOG_TAG, "Cart has " + mCartItems.size() + " items.");
        }
    }

    // Megrendelés / Fizetés folyamata
    private void checkout() {
        Log.i(LOG_TAG, "Checkout button clicked.");
        if (mCartItems.isEmpty()) {
            Toast.makeText(this, "A kosár üres, nincs mit megrendelni.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Kosár ürítése a CartManagerben
        CartManager.getInstance().clearCart();

        // 2. Visszajelzés a felhasználónak
        Toast.makeText(this, "Megrendelés leadva! Köszönjük a vásárlást!", Toast.LENGTH_LONG).show();

        // 3. Activity bezárása és visszatérés az ArtListActivity-hez
        finish();
    }

    private void updateTotalPrice() {
        String formattedTotal;
        long totalPrice = CartManager.getInstance().getTotalPrice();
        try {
            Locale hungarianLocale = Locale.forLanguageTag("hu-HU");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(hungarianLocale);

            currencyFormatter.setMaximumFractionDigits(0);

            currencyFormatter.setCurrency(Currency.getInstance("HUF"));

            formattedTotal = currencyFormatter.format(totalPrice);

        } catch (IllegalArgumentException | NullPointerException e) {
            Log.e(LOG_TAG, "Error formatting total price: " + totalPrice, e);
            // Fallback: Egyszerűen kiírjuk a számot Ft-vel, formázás nélkül
            formattedTotal = String.format(Locale.US, "%.0f Ft", totalPrice);
        }

        // 3. TextView frissítése a formázott árral
        if (mTotalPriceTextView != null) {
            mTotalPriceTextView.setText(formattedTotal);
            mTotalLabelTextView.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "Total price updated UI: " + formattedTotal);
        } else {
            Log.e(LOG_TAG, "mTotalPriceTextView is null, cannot update UI.");
        }

    }
}