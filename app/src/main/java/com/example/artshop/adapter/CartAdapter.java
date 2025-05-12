package com.example.artshop.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Eltávolítás visszajelzéshez

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.artshop.CartActivity; // Szülő Activity referencia (opcionális)
import com.example.artshop.R;
import com.example.artshop.logic.CartManager; // CartManager használata
import com.example.artshop.model.ArtworkItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private static final String LOG_TAG = CartAdapter.class.getSimpleName();
    private List<ArtworkItem> mCartItems;
    private Context mContext;
    private LayoutInflater mInflater;

    public CartAdapter(Context context, List<ArtworkItem> cartItems) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mCartItems = cartItems;
        Log.d(LOG_TAG, "CartAdapter created with " + mCartItems.size() + " items.");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArtworkItem currentItem = mCartItems.get(position);
        holder.bindTo(currentItem);
    }

    @Override
    public int getItemCount() {
        return mCartItems.size();
    }

    public void updateCartItems(List<ArtworkItem> newItems) {
        mCartItems.clear();
        mCartItems.addAll(newItems);
        notifyDataSetChanged();
        Log.d(LOG_TAG, "CartAdapter data updated. New size: " + mCartItems.size());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        TextView mTitleTextView;
        TextView mPriceTextView;
        ImageButton mRemoveButton;

        ViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.imageViewCartItem);
            mTitleTextView = itemView.findViewById(R.id.textViewCartItemTitle);
            mPriceTextView = itemView.findViewById(R.id.textViewCartItemPrice);
            mRemoveButton = itemView.findViewById(R.id.buttonRemoveFromCart);

            // Eltávolítás gomb listener
            mRemoveButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArtworkItem itemToRemove = mCartItems.get(position);
                    if (itemToRemove != null && itemToRemove.getId() != null) {
                        Log.d(LOG_TAG, "Remove clicked for: " + itemToRemove.getTitle());

                        // Eltávolítás a CartManagerből
                        CartManager.getInstance().removeItem(itemToRemove.getId());

                        if (mContext instanceof CartActivity) {
                            ((CartActivity) mContext).refreshCartList();
                        }
                        Toast.makeText(mContext, "'" + itemToRemove.getTitle() + "' eltávolítva a kosárból.", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.e(LOG_TAG, "Cannot remove item: item or ID is null at position " + position);
                    }
                }
            });
        }

        void bindTo(ArtworkItem currentItem) {
            mTitleTextView.setText(currentItem.getTitle());

            // Ár formázása (ugyanaz az egyszerű logika, mint a másik adapterben)
            String formattedPrice;
            long price = currentItem.getPrice();
            try {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("hu", "HU"));
                symbols.setGroupingSeparator(' ');
                DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
                formattedPrice = formatter.format(price) + " Ft";

            } catch (NumberFormatException | NullPointerException e) {
                Log.e(LOG_TAG, "Error formatting price: ", e);
                formattedPrice = " (Hiba)";
            }
            mPriceTextView.setText(formattedPrice);

            // Kép betöltése
            Glide.with(mContext)
                    .load(currentItem.getImageUrl())
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_broken_image_24)
                    .fallback(R.drawable.baseline_search_24)
                    .into(mImageView);
        }
    }
}