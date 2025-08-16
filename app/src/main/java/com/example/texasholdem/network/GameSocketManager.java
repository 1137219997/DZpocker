package com.example.texasholdem.network;

import android.util.Log;

import com.example.texasholdem.models.Card;
import com.example.texasholdem.models.GameState;
import com.example.texasholdem.models.Player;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GameSocketManager {
    private static final String TAG = "GameSocketManager";
    private static final String SERVER_URL = "http://10.0.2.2:3000"; // Android模拟器使用10.0.2.2访问localhost
    
    private Socket socket;
    private GameSocketListener listener;
    private String currentRoomId;
    private String currentPlayerId;
    
    public interface GameSocketListener {
        void onConnected();
        void onDisconnected();
        void onJoinedRoom(boolean success, String message, GameState gameState);
        void onPlayerJoined(Player player, GameState gameState);
        void onPlayerLeft(String playerId, GameState gameState);
        void onGameStarted(GameState gameState);
        void onGameStateUpdated(GameState gameState);
        void onError(String error);
    }
    
    public GameSocketManager(GameSocketListener listener) {
        this.listener = listener;
        initializeSocket();
    }
    
    private void initializeSocket() {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;
            
            socket = IO.socket(SERVER_URL, options);
            setupEventListeners();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error initializing socket: " + e.getMessage());
            if (listener != null) {
                listener.onError("Socket initialization failed: " + e.getMessage());
            }
        }
    }
    
    private void setupEventListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Connected to server");
            if (listener != null) {
                listener.onConnected();
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Disconnected from server");
            if (listener != null) {
                listener.onDisconnected();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Connection error: " + args[0]);
            if (listener != null) {
                listener.onError("Connection error: " + args[0]);
            }
        });
        
        socket.on("joinedRoom", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                boolean success = data.getBoolean("success");
                String message = data.optString("message", "");
                GameState gameState = null;
                
                Log.d(TAG, "joinedRoom event received: " + data.toString());
                
                if (success && data.has("gameState")) {
                    gameState = parseGameState(data.getJSONObject("gameState"));
                    if (data.has("playerId")) {
                        currentPlayerId = data.getString("playerId");
                    }
                    if (data.has("currentPlayerId")) {
                        currentPlayerId = data.getString("currentPlayerId");
                    }
                    
                    Log.d(TAG, "Current player ID set to: " + currentPlayerId);
                    if (gameState != null && gameState.getPlayers() != null) {
                        Log.d(TAG, "Game state contains " + gameState.getPlayers().size() + " players");
                        for (Player p : gameState.getPlayers()) {
                            Log.d(TAG, "Player: " + p.getName() + " (ID: " + p.getId() + ")");
                        }
                    }
                }
                
                if (listener != null) {
                    listener.onJoinedRoom(success, message, gameState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing joinedRoom event: " + e.getMessage());
            }
        });
        
        socket.on("playerJoined", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Log.d(TAG, "playerJoined event received: " + data.toString());
                
                Player player = null;
                if (data.has("player") && !data.isNull("player")) {
                    player = parsePlayer(data.getJSONObject("player"));
                }
                
                GameState gameState = null;
                if (data.has("gameState") && !data.isNull("gameState")) {
                    gameState = parseGameState(data.getJSONObject("gameState"));
                    if (gameState != null && gameState.getPlayers() != null) {
                        Log.d(TAG, "playerJoined game state contains " + gameState.getPlayers().size() + " players");
                        for (Player p : gameState.getPlayers()) {
                            Log.d(TAG, "Player: " + p.getName() + " (ID: " + p.getId() + ")");
                        }
                    }
                }
                
                if (listener != null) {
                    listener.onPlayerJoined(player, gameState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing playerJoined event: " + e.getMessage());
                if (listener != null) {
                    listener.onPlayerJoined(null, null);
                }
            }
        });
        
        socket.on("playerLeft", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String playerId = data.getString("playerId");
                GameState gameState = parseGameState(data.getJSONObject("gameState"));
                
                if (listener != null) {
                    listener.onPlayerLeft(playerId, gameState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing playerLeft event: " + e.getMessage());
            }
        });
        
        socket.on("gameStarted", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                GameState gameState = parseGameState(data.getJSONObject("gameState"));
                
                if (data.has("currentPlayerId")) {
                    currentPlayerId = data.getString("currentPlayerId");
                }
                
                if (listener != null) {
                    listener.onGameStarted(gameState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing gameStarted event: " + e.getMessage());
            }
        });
        
        socket.on("gameStateUpdated", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                GameState gameState = parseGameState(data.getJSONObject("gameState"));
                
                if (data.has("currentPlayerId")) {
                    currentPlayerId = data.getString("currentPlayerId");
                }
                
                if (listener != null) {
                    listener.onGameStateUpdated(gameState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing gameStateUpdated event: " + e.getMessage());
            }
        });
    }
    
    public void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }
    
    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }
    
    public void joinRoom(String roomId, String playerName) {
        if (socket != null && socket.connected()) {
            currentRoomId = roomId;
            
            JSONObject data = new JSONObject();
            try {
                data.put("roomId", roomId);
                data.put("playerName", playerName);
                socket.emit("joinRoom", data);
                Log.d(TAG, "Joining room: " + roomId + " as " + playerName);
            } catch (Exception e) {
                Log.e(TAG, "Error joining room: " + e.getMessage());
            }
        }
    }
    
    public void startGame() {
        if (socket != null && socket.connected() && currentRoomId != null) {
            JSONObject data = new JSONObject();
            try {
                data.put("roomId", currentRoomId);
                socket.emit("startGame", data);
            } catch (Exception e) {
                Log.e(TAG, "Error starting game: " + e.getMessage());
            }
        }
    }
    
    public void makeBet(String action, int amount) {
        if (socket != null && socket.connected() && currentRoomId != null) {
            JSONObject data = new JSONObject();
            try {
                data.put("roomId", currentRoomId);
                data.put("action", action);
                if (amount > 0) {
                    data.put("amount", amount);
                }
                socket.emit("makeBet", data);
            } catch (Exception e) {
                Log.e(TAG, "Error making bet: " + e.getMessage());
            }
        }
    }
    
    public void fold() {
        makeBet("fold", 0);
    }
    
    public void call() {
        makeBet("call", 0);
    }
    
    public void raise(int amount) {
        makeBet("raise", amount);
    }
    
    public void allIn() {
        makeBet("allIn", 0);
    }
    
    private GameState parseGameState(JSONObject json) {
        try {
            GameState gameState = new GameState();
            gameState.setRoomId(json.getString("roomId"));
            gameState.setPot(json.getInt("pot"));
            gameState.setCurrentBet(json.getInt("currentBet"));
            gameState.setCurrentPlayerIndex(json.getInt("currentPlayerIndex"));
            gameState.setGamePhase(json.getString("gamePhase"));
            gameState.setDealerIndex(json.getInt("dealerIndex"));
            
            // Parse players
            List<Player> players = new ArrayList<>();
            if (json.has("players")) {
                JSONArray playersArray = json.getJSONArray("players");
                for (int i = 0; i < playersArray.length(); i++) {
                    JSONObject playerJson = playersArray.getJSONObject(i);
                    Player player = parsePlayer(playerJson);
                    if (player != null) {
                        players.add(player);
                    }
                }
            }
            gameState.setPlayers(players);
            
            // Parse community cards
            List<Card> communityCards = new ArrayList<>();
            if (json.has("communityCards")) {
                JSONArray cardsArray = json.getJSONArray("communityCards");
                for (int i = 0; i < cardsArray.length(); i++) {
                    JSONObject cardJson = cardsArray.getJSONObject(i);
                    Card card = parseCard(cardJson);
                    if (card != null) {
                        communityCards.add(card);
                    }
                }
            }
            gameState.setCommunityCards(communityCards);
            
            return gameState;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing game state: " + e.getMessage());
            return null;
        }
    }
    
    private Player parsePlayer(JSONObject json) {
        try {
            if (json == null) {
                Log.e(TAG, "parsePlayer: JSON object is null");
                return null;
            }
            
            Log.d(TAG, "parsePlayer: parsing JSON: " + json.toString());
            
            String id = json.getString("id");
            String name = json.getString("name");
            Player player = new Player(id, name);
            
            // 设置默认值，避免JSON中缺少字段时的错误
            player.setChips(json.optInt("chips", 1000));
            player.setBet(json.optInt("bet", 0));
            player.setFolded(json.optBoolean("folded", false));
            player.setAllIn(json.optBoolean("allIn", false));
            
            // 解析当前玩家和庄家标识
            player.setCurrentPlayer(json.optBoolean("isCurrentPlayer", false));
            player.setDealer(json.optBoolean("isDealer", false));
            
            Log.d(TAG, "parsePlayer: player " + name + " - isCurrentPlayer: " + player.isCurrentPlayer() + ", isDealer: " + player.isDealer());
            
            // Parse hand cards
            List<Card> hand = new ArrayList<>();
            if (json.has("hand") && !json.isNull("hand")) {
                JSONArray handArray = json.getJSONArray("hand");
                for (int i = 0; i < handArray.length(); i++) {
                    JSONObject cardJson = handArray.getJSONObject(i);
                    Card card = parseCard(cardJson);
                    if (card != null) {
                        hand.add(card);
                    }
                }
            }
            player.setHand(hand);
            
            Log.d(TAG, "parsePlayer: successfully parsed player: " + name);
            return player;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing player: " + e.getMessage());
            Log.e(TAG, "JSON content: " + (json != null ? json.toString() : "null"));
            return null;
        }
    }
    
    private Card parseCard(JSONObject json) {
        try {
            String suit = json.getString("suit");
            String rank = json.getString("rank");
            int value = json.getInt("value");
            Card card = new Card(suit, rank, value);
            card.setVisible(true); // 默认显示卡牌
            return card;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing card: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isConnected() {
        return socket != null && socket.connected();
    }
    
    public String getCurrentRoomId() {
        return currentRoomId;
    }
    
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
} 