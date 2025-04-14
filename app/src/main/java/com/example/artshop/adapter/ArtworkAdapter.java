package com.example.artshop.adapter;

import android.content.Context;
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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast; // Kosárba rakás visszajelzéshez

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Képbetöltéshez
import com.example.artshop.ArtListActivity; // Kosár ikon frissítéséhez
import com.example.artshop.R;
import com.example.artshop.model.ArtworkItem;

import java.util.ArrayList;
import java.text.Normalizer;
import java.util.Locale; // Szűréshez

public class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ViewHolder> implements Filterable {

    private static final String LOG_TAG = ArtworkAdapter.class.getName();

    // Adatlisták (eredeti és szűrt)
    private ArrayList<ArtworkItem> mArtworkData;
    private ArrayList<ArtworkItem> mArtworkDataAll; // A teljes lista a szűréshez
    private Context mContext;
    private int lastPosition = -1; // Animációhoz
    private boolean shouldAnimate = true;

    public ArtworkAdapter(Context context, ArrayList<ArtworkItem> itemsData) {
        this.mContext = context;
        this.mArtworkData = itemsData;
        this.mArtworkDataAll = new ArrayList<>(itemsData);
        Log.d(LOG_TAG, "Adapter created with " + itemsData.size() + " items.");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.v(LOG_TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_artwork, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArtworkItem currentItem = mArtworkData.get(position);
        holder.bindTo(currentItem);


        // Csak akkor alkalmazzuk az animációt, ha shouldAnimate true (keresés közbeni animáció megadakályozására)
        if (shouldAnimate && holder.getAbsoluteAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAbsoluteAdapterPosition();
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
        private TextView mTitleText;
        private TextView mDescriptionText;
        private TextView mPriceText;
        private ImageView mItemImage;
        private RatingBar mRatingBar;
        private TextView mArtistText; // Új mező az alkotóhoz
        private Button mAddToCartButton;

        ViewHolder(View itemView) {
            super(itemView);

            mTitleText = itemView.findViewById(R.id.artworkTitle);
            mDescriptionText = itemView.findViewById(R.id.artworkDescription);
            mPriceText = itemView.findViewById(R.id.artworkPrice);
            mItemImage = itemView.findViewById(R.id.artworkImage);
            mRatingBar = itemView.findViewById(R.id.artworkRating);
            mArtistText = itemView.findViewById(R.id.artworkArtist);
            mAddToCartButton = itemView.findViewById(R.id.buttonAddToCart);

            // Kosárba gomb eseménykezelője
            mAddToCartButton.setOnClickListener(v -> {
                int position = getAbsoluteAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArtworkItem clickedItem = mArtworkData.get(position);
                    Log.d(LOG_TAG, "Add to cart clicked for: " + clickedItem.getTitle());
                    Toast.makeText(mContext, clickedItem.getTitle() + " kosárba helyezve!", Toast.LENGTH_SHORT).show();

                    // Frissítjük a kosár ikont az Activity-ben
                    if (mContext instanceof ArtListActivity) {
                        ((ArtListActivity) mContext).updateCartIcon();
                    }

                    // Itt lehetne a tényleges kosár logika (pl. hozzáadás Firestore-hoz)
                    // CRUD - Update (pl. kosár darabszám növelése) vagy Create (új elem a kosár kollekcióba)
                }
            });

            // Elemre kattintás (később részletek megjelenítéséhez)
            itemView.setOnClickListener(view -> {
                int position = getAbsoluteAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArtworkItem clickedItem = mArtworkData.get(position);
                    Log.d(LOG_TAG, "Item clicked: " + clickedItem.getTitle());
                    Toast.makeText(mContext, "Részletek: Terméknézet még nincs implementálva", Toast.LENGTH_SHORT).show();
                    // Intent intent = new Intent(mContext, ArtworkDetailActivity.class);
                    // intent.putExtra("ARTWORK_ID", clickedItem.getId()); // Átadjuk az ID-t
                    // mContext.startActivity(intent);
                }
            });
        }

        void bindTo(ArtworkItem currentItem) {
            mTitleText.setText(currentItem.getTitle());
            mDescriptionText.setText(currentItem.getDescription());
            mPriceText.setText(currentItem.getPrice());
            mRatingBar.setRating(currentItem.getRating());
            mArtistText.setText(mContext.getString(R.string.artist_label, currentItem.getArtist()));

            // Kép betöltése Glide-dal (kezeli a gyorsítótárazást, átméretezést)
            Glide.with(mContext)
                    .load(currentItem.getImageResource())
                    .placeholder(R.drawable.baseline_image_24) // Placeholder kép betöltés alatt
                    .error(R.drawable.baseline_broken_image_24) // Hiba esetén megjelenő kép
                    .into(mItemImage);
        }
    }

    // --- Filterable Implementáció ---
    @Override
    public Filter getFilter() {
        return artworkFilter;
    }

    private Filter artworkFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<ArtworkItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if (mArtworkDataAll == null) {
                results.count = 0;
                results.values = filteredList;
                return results;
            }

            // Ékezetek eltávolítása, kisbetűssé aalkítás
            String filterPattern = (constraint == null) ? "" : removeAccents(constraint.toString().toLowerCase(Locale.ROOT)).trim();

            if (filterPattern.isEmpty()) {
                results.count = mArtworkDataAll.size();
                results.values = mArtworkDataAll;
            } else {
                for (ArtworkItem item : mArtworkDataAll) {
                    String titleLower = removeAccents(item.getTitle() != null ? item.getTitle().toLowerCase(Locale.ROOT) : "");
                    String descLower = removeAccents(item.getDescription() != null ? item.getDescription().toLowerCase(Locale.ROOT) : "");
                    String artistLower = removeAccents(item.getArtist() != null ? item.getArtist().toLowerCase(Locale.ROOT) : "");

                    if (titleLower.contains(filterPattern) || descLower.contains(filterPattern) || artistLower.contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Töröljük az aktuálisan megjelenített listát (mArtworkData)
            mArtworkData.clear();
            if (results.values != null) {
                try {
                    mArtworkData.addAll((ArrayList<ArtworkItem>) results.values);
                    Log.d(LOG_TAG, "Publishing filter results, new data size: " + mArtworkData.size());
                } catch (ClassCastException | NullPointerException e) {
                    Log.e(LOG_TAG, "Error casting or adding filter results", e);
                    if (mArtworkDataAll != null) {
                        mArtworkData.addAll(mArtworkDataAll);
                    }
                }
            } else {
                Log.w(LOG_TAG, "Filter results values are null. Check performFiltering.");
                if (mArtworkDataAll != null) {
                    mArtworkData.addAll(mArtworkDataAll);
                }
            }

            if (mArtworkData.isEmpty()) {
                Log.i(LOG_TAG, "No items to display after filtering.");
                // Itt jelezhetünk a felhasználónak, pl. egy TextView láthatóvá tételével
                Toast.makeText(mContext, R.string.empty_list_message, Toast.LENGTH_SHORT).show();
            }
            notifyDataSetChanged();
            lastPosition = -1;
        }
    };

    // Ha kívülről frissíted az adatokat (pl. Firestore lekérdezés után)
    public void updateData(ArrayList<ArtworkItem> newData) {
        mArtworkData.clear();
        mArtworkData.addAll(newData);
        mArtworkDataAll = new ArrayList<>(newData);
        notifyDataSetChanged();
        lastPosition = -1;
        Log.i(LOG_TAG, "Adapter data updated externally. mArtworkDataAll size: " + mArtworkDataAll.size());
    }

    private static String removeAccents(String text) {
        if (text == null) {
            return null;
        }
        // Normalizálás NFD formátumban, majd az ékezetek eltávolítása (unicode mark karakterek)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}