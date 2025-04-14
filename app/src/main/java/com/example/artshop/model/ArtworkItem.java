package com.example.artshop.model;

public class ArtworkItem {
    private String id; // Firestore dokumentum ID-hoz (később Firestore-hoz)
    private String title;
    private String description;
    private String price;
    private float rating;
    private int imageResource; // Helyi kép erőforrás ID (később lehet URL a Firebase Storage-ból)
    private String artist;

    // Üres konstruktor a Firestore számára (később lesz fontos)
    public ArtworkItem() {}

    public ArtworkItem(String title, String description, String price, float rating, int imageResource, String artist) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.imageResource = imageResource;
        this.artist = artist;
    }

    // --- Getters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // Később kellhet a Firestore ID beállításához

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public int getImageResource() { return imageResource; }
    public String getArtist() { return artist; }
}