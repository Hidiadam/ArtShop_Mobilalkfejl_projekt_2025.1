package com.example.artshop.logic;

import android.util.Log;

import com.example.artshop.model.ArtworkItem;
import java.util.ArrayList;
import java.util.List;

public class CartManager {

    private static final String LOG_TAG = CartManager.class.getSimpleName();
    private static CartManager instance;
    private List<ArtworkItem> cartItems;
    private CartManager() {
        cartItems = new ArrayList<>();
    }

    // Metódus az instance lekéréséhez
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    // Elem hozzáadása a kosárhoz
    public void addItem(ArtworkItem item) {
        if (item != null && !isItemInCart(item.getId())) {
            cartItems.add(item);
            Log.i(LOG_TAG, "Item added to cart: " + item.getTitle() + " (ID: " + item.getId() + ")");
        } else if (item != null) {
            Log.w(LOG_TAG, "Item already in cart: " + item.getTitle());
        } else {
            Log.e(LOG_TAG, "Attempted to add null item to cart.");
        }
    }

    // Elem eltávolítása a kosárból ID alapján
    public void removeItem(String itemId) {
        if (itemId == null) return;
        ArtworkItem itemToRemove = null;
        for (ArtworkItem item : cartItems) {
            if (itemId.equals(item.getId())) {
                itemToRemove = item;
                break;
            }
        }
        if (itemToRemove != null) {
            cartItems.remove(itemToRemove);
            Log.i(LOG_TAG, "Item removed from cart: " + itemToRemove.getTitle() + " (ID: " + itemId + ")");
        } else {
            Log.w(LOG_TAG, "Attempted to remove item not found in cart. ID: " + itemId);
        }
    }

    // Kosár tartalmának lekérése (másolatot ad vissza, hogy kívülről ne módosíthassák közvetlenül)
    public List<ArtworkItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    // Kosárban lévő elemek számának lekérése
    public int getCartItemCount() {
        return cartItems.size();
    }

    // Kosár ürítése
    public void clearCart() {
        cartItems.clear();
        Log.i(LOG_TAG, "Cart cleared.");
    }

    // Ellenőrzi, hogy egy adott ID-jú elem már a kosárban van-e
    private boolean isItemInCart(String itemId) {
        if (itemId == null) return false;
        for (ArtworkItem item : cartItems) {
            if (itemId.equals(item.getId())) {
                return true;
            }
        }
        return false;
    }


    public long getTotalPrice() {
        long total = 0;
        for (ArtworkItem item : cartItems) {
            if (item != null) {
                total += item.getPrice();
            } else {
                Log.w(LOG_TAG,"Null item encountered while calculating total price.");
            }
        }
        Log.d(LOG_TAG, "Calculated total price: " + total);
        return total;
    }

}