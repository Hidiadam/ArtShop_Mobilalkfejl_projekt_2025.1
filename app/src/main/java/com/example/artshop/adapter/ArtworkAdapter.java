package com.example.artshop.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.artshop.AddEditArtworkActivity; // ÚJ Activity
import com.example.artshop.ArtListActivity;
import com.example.artshop.R;
import com.example.artshop.model.ArtworkItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List; // Fontos: List-et használjunk a rugalmasságért
import java.util.Locale;

import com.example.artshop.logic.CartManager; // ÚJ import
import com.example.artshop.ArtworkDetailActivity; // ÚJ import
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;

public class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ViewHolder> {

    private static final String LOG_TAG = ArtworkAdapter.class.getName();

    private List<ArtworkItem> mArtworkData; // THIS WILL BE THE SHARED LIST REFERENCE
    private List<ArtworkItem> mArtworkDataAll; // This remains a copy for filtering
    private Context mContext;
    private int lastPosition = -1;
    private boolean shouldAnimate = true;
    private LayoutInflater mInflater;

    public ArtworkAdapter(Context context, List<ArtworkItem> itemsData) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mArtworkData = itemsData;
        this.mArtworkDataAll = new ArrayList<>(itemsData); // Teljes lista is másolat
        Log.d(LOG_TAG, "Adapter created initially with " + itemsData.size() + " items.");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.v(LOG_TAG, "onCreateViewHolder called");
        View view = mInflater.inflate(R.layout.list_item_artwork, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArtworkItem currentItem = mArtworkData.get(position);
        if (currentItem != null) {
            holder.bindTo(currentItem);

            if (shouldAnimate && holder.getAbsoluteAdapterPosition() > lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
                holder.itemView.startAnimation(animation);
                lastPosition = holder.getAbsoluteAdapterPosition();
            }
        } else {
            Log.e(LOG_TAG, "Error: currentItem is null at position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return mArtworkData.size();
    }

    public void setShouldAnimate(boolean animate) {
        this.shouldAnimate = animate;
    }

    // --- ViewHolder Belső Osztály ---
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitleText;
        TextView mDescriptionText;
        TextView mPriceText;
        ImageView mItemImage;
        RatingBar mRatingBar;
        TextView mArtistText;
        Button mAddToCartButton;
        Button mEditButton;
        Button mDeleteButton;
        ProgressBar mProgressBar;

        ViewHolder(View itemView) {
            super(itemView);
            mTitleText = itemView.findViewById(R.id.artworkTitle);
            mDescriptionText = itemView.findViewById(R.id.artworkDescription);
            mPriceText = itemView.findViewById(R.id.artworkPrice);
            mItemImage = itemView.findViewById(R.id.artworkImage);
            mRatingBar = itemView.findViewById(R.id.artworkRating);
            mArtistText = itemView.findViewById(R.id.artworkArtist);
            mAddToCartButton = itemView.findViewById(R.id.buttonAddToCart);
            mEditButton = itemView.findViewById(R.id.buttonEdit);
            mDeleteButton = itemView.findViewById(R.id.buttonDelete);
            mProgressBar = itemView.findViewById(R.id.progressBarListItem);

            // --- Listener-ek ---
            mAddToCartButton.setOnClickListener(v -> handleAddToCart());
            mEditButton.setOnClickListener(v -> handleEdit());
            mDeleteButton.setOnClickListener(v -> handleDelete());
            itemView.setOnClickListener(v -> handleItemClick());
        }

        private void handleAddToCart() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                ArtworkItem clickedItem = mArtworkData.get(position);
                Log.d(LOG_TAG, "Add to cart clicked for: " + clickedItem.getTitle());

                CartManager.getInstance().addItem(clickedItem);


                Toast.makeText(mContext, clickedItem.getTitle() + " kosárba helyezve!", Toast.LENGTH_SHORT).show();
                if (mContext instanceof ArtListActivity) {
                    ((ArtListActivity) mContext).updateCartIconCount(CartManager.getInstance().getCartItemCount());
                }


            }
        }

        private void handleEdit() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && mContext instanceof ArtListActivity) {
                ArtworkItem clickedItem = mArtworkData.get(position);
                Log.d(LOG_TAG, "Edit clicked for ID: " + clickedItem.getId());
                ((ArtListActivity) mContext).editArtwork(clickedItem.getId());
            }
        }

        private void handleDelete() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && mContext instanceof ArtListActivity) {
                ArtworkItem clickedItem = mArtworkData.get(position);
                Log.d(LOG_TAG, "Delete clicked for ID: " + clickedItem.getId());

                // Megerősítő dialógus megjelenítése
                new AlertDialog.Builder(mContext)
                        .setTitle("Törlés megerősítése")
                        .setMessage("Biztosan törölni szeretnéd ezt a műalkotást: '" + clickedItem.getTitle() + "'?")
                        .setPositiveButton("Igen", (dialog, which) -> {
                            // Csak akkor hívjuk meg a törlést, ha a felhasználó megerősítette
                            ((ArtListActivity) mContext).deleteArtwork(clickedItem);
                        })
                        .setNegativeButton("Nem", null) // Nem gomb nem csinál semmit
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }

        private void handleItemClick() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                ArtworkItem clickedItem = mArtworkData.get(position);
                if (clickedItem != null && clickedItem.getId() != null) {
                    Log.d(LOG_TAG, "Item clicked: " + clickedItem.getTitle() + " (ID: " + clickedItem.getId() + ")");
                    Intent intent = new Intent(mContext, ArtworkDetailActivity.class);
                    // Átadjuk a Firestore dokumentum ID-t az új Activity-nek
                    intent.putExtra(ArtworkDetailActivity.EXTRA_DETAIL_ARTWORK_ID, clickedItem.getId());
                    mContext.startActivity(intent);
                } else {
                    Log.e(LOG_TAG, "Clicked item or its ID is null at position: " + position);
                    Toast.makeText(mContext, "Hiba: A műalkotás részletei nem érhetők el.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        void bindTo(ArtworkItem currentItem) {
            mTitleText.setText(currentItem.getTitle());
            mDescriptionText.setText(currentItem.getDescription());

            String formattedPrice;
            long price = currentItem.getPrice();

                try {
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("hu", "HU"));
                    symbols.setGroupingSeparator(' '); // Already default for hu_HU, but explicit doesn't hurt
                    DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
                    formattedPrice = formatter.format(price) + " Ft";

                } catch (NumberFormatException | NullPointerException e) {
                    Log.e(LOG_TAG, "Error formatting price: ", e);
                    formattedPrice = " (Hiba)";
                }

            mPriceText.setText(formattedPrice);

            mRatingBar.setRating(currentItem.getRating());
            mArtistText.setText(mContext.getString(R.string.artist_label, currentItem.getArtist()));


            // --- START: REPLACEMENT FOR GLIDE CALL ---
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
            if (mItemImage != null) {
                mItemImage.setVisibility(View.INVISIBLE);
            }


            Glide.with(mContext)
                    .load(currentItem.getImageUrl())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                            if (mItemImage != null) mItemImage.setVisibility(View.VISIBLE);
                            Log.w(LOG_TAG, "Glide failed: " + currentItem.getImageUrl(), e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                            if (mItemImage != null) mItemImage.setVisibility(View.VISIBLE);
                            Log.v(LOG_TAG, "Glide loaded: " + currentItem.getImageUrl());
                            return false;
                        }
                    })
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_broken_image_24)
                    .fallback(R.drawable.baseline_image_24)
                    .into(mItemImage);
        }
    }


    public void updateData(List<ArtworkItem> newData) {
        if (newData == null) {
            Log.w(LOG_TAG, "Attempted to update adapter with null data.");
            newData = new ArrayList<>();
        }
        mArtworkDataAll.clear();
        mArtworkDataAll.addAll(newData);
        mArtworkData.clear();
        mArtworkData.addAll(newData);

        Log.i(LOG_TAG, "Adapter data updated externally. New total size: " + mArtworkDataAll.size());
        notifyDataSetChanged();
        lastPosition = -1;

        if(mContext instanceof ArtListActivity) {
            ((ArtListActivity)mContext).showEmptyListMessage(mArtworkData.isEmpty());
        }
    }

    // Ékezeteket eltávolító segédfüggvény
    private static String removeAccents(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }


}