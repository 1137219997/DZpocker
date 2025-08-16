package com.example.texasholdem.models;

import java.util.List;

public class Player {
    private String id;
    private String name;
    private int chips;
    private List<Card> hand;
    private int bet;
    private boolean folded;
    private boolean allIn;
    private boolean isCurrentPlayer;
    private boolean isDealer;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.chips = 1000;
        this.bet = 0;
        this.folded = false;
        this.allIn = false;
        this.isCurrentPlayer = false;
        this.isDealer = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }

    public boolean isCurrentPlayer() {
        return isCurrentPlayer;
    }

    public void setCurrentPlayer(boolean currentPlayer) {
        isCurrentPlayer = currentPlayer;
    }

    public boolean isDealer() {
        return isDealer;
    }

    public void setDealer(boolean dealer) {
        isDealer = dealer;
    }

    public void addChips(int amount) {
        this.chips += amount;
    }

    public void removeChips(int amount) {
        if (this.chips >= amount) {
            this.chips -= amount;
        }
    }

    public void placeBet(int amount) {
        if (amount <= this.chips) {
            this.chips -= amount;
            this.bet += amount;
        }
    }

    public void resetBet() {
        this.bet = 0;
    }

    public String getStatusText() {
        if (folded) return "弃牌";
        if (allIn) return "全下";
        if (bet > 0) return "下注: " + bet;
        return "等待中";
    }
} 