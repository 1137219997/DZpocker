package com.example.texasholdem.models;

import java.util.List;

public class GameState {
    private String roomId;
    private List<Player> players;
    private List<Card> communityCards;
    private int pot;
    private int currentBet;
    private int currentPlayerIndex;
    private String gamePhase;
    private int dealerIndex;

    public GameState() {
    }

    public GameState(String roomId, List<Player> players, List<Card> communityCards, 
                    int pot, int currentBet, int currentPlayerIndex, 
                    String gamePhase, int dealerIndex) {
        this.roomId = roomId;
        this.players = players;
        this.communityCards = communityCards;
        this.pot = pot;
        this.currentBet = currentBet;
        this.currentPlayerIndex = currentPlayerIndex;
        this.gamePhase = gamePhase;
        this.dealerIndex = dealerIndex;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Card> getCommunityCards() {
        return communityCards;
    }

    public void setCommunityCards(List<Card> communityCards) {
        this.communityCards = communityCards;
    }

    public int getPot() {
        return pot;
    }

    public void setPot(int pot) {
        this.pot = pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public String getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(String gamePhase) {
        this.gamePhase = gamePhase;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    public String getGamePhaseText() {
        switch (gamePhase) {
            case "waiting": return "等待玩家";
            case "preflop": return "翻牌前";
            case "flop": return "翻牌";
            case "turn": return "转牌";
            case "river": return "河牌";
            case "showdown": return "摊牌";
            default: return gamePhase;
        }
    }

    public Player getCurrentPlayer() {
        if (players != null && currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    public Player getDealer() {
        if (players != null && dealerIndex >= 0 && dealerIndex < players.size()) {
            return players.get(dealerIndex);
        }
        return null;
    }

    public boolean isGameActive() {
        return !"waiting".equals(gamePhase) && !"showdown".equals(gamePhase);
    }

    public int getActivePlayerCount() {
        if (players == null) return 0;
        return (int) players.stream().filter(p -> !p.isFolded()).count();
    }
} 