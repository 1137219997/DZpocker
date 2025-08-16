package com.example.texasholdem.models;

public class Card {
    private String suit;
    private String rank;
    private int value;
    private boolean isVisible;

    public Card(String suit, String rank, int value) {
        this.suit = suit;
        this.rank = rank;
        this.value = value;
        this.isVisible = false;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public String getDisplayName() {
        if (!isVisible) {
            return "?";
        }
        return rank + " of " + suit;
    }

    public String getSuitSymbol() {
        switch (suit.toLowerCase()) {
            case "hearts": return "♥";
            case "diamonds": return "♦";
            case "clubs": return "♣";
            case "spades": return "♠";
            default: return suit;
        }
    }

    public int getSuitColor() {
        switch (suit.toLowerCase()) {
            case "hearts":
            case "diamonds":
                return 0xFFE74C3C; // 红色
            case "clubs":
            case "spades":
                return 0xFF2C3E50; // 黑色
            default:
                return 0xFF000000;
        }
    }
} 