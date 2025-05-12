package com.example.artshop.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp; // Timestamphez
import java.util.Date; // Timestamphez
import java.util.Locale; // Szükséges a toLowerCase-hez
import java.text.Normalizer; // Szükséges az ékezet eltávolításhoz

public class ArtworkItem {

    @DocumentId // Ezzel jelezzük, hogy ez a mező tárolja a Firestore ID-t
    private String id;
    private String title;
    private String titleLower;
    private String description;
    private long price; // Árat érdemes lehet Number-ként tárolni Firestore-ban a pontosabb lekérdezéshez/rendezéshez
    private float rating;
    private String imageUrl;
    private String artist;
    private String artistLower;
    @ServerTimestamp
    private Date createdAt;
    private String userId;

    public ArtworkItem() {}

    public ArtworkItem(String title, String artist, String description, long price, float rating, String imageUrl, String userId) {
        this.title = title;
        setTitle(title);
        setArtist(artist);
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.userId = userId;
        // this.createdAt-ot a Firestore automatikusan beállítja @ServerTimestamp miatt
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getTitleLower() { return titleLower; }
    public String getDescription() { return description; }
    public long getPrice() { return price; }
    public float getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getArtist() { return artist; }
    public String getArtistLower() { return artistLower; }
    public Date getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }

    // --- Setters (Firestore-nak lehet, hogy szüksége van rájuk, de manuálisan ritkán hívjuk) ---
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title;
        this.titleLower = normalizeText(title);
    }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(long price) { this.price = price; }
    public void setRating(float rating) { this.rating = rating; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setArtist(String artist) { this.artist = artist;
        this.artistLower = normalizeText(artist);
    }
    public void setArtistLower(String artistLower) { this.artistLower = artistLower; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUserId(String userId) { this.userId = userId; }

    private static String normalizeText(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT); // Kisbetűsítés is itt történik
    }
}