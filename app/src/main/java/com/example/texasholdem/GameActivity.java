package com.example.texasholdem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.texasholdem.models.Card;
import com.example.texasholdem.models.GameState;
import com.example.texasholdem.models.Player;
import com.example.texasholdem.network.GameSocketManager;
import com.example.texasholdem.views.CardView;
import com.example.texasholdem.views.PlayerView;

import java.util.List;

public class GameActivity extends AppCompatActivity implements GameSocketManager.GameSocketListener {
    
    private String playerName;
    private String roomId;
    private GameSocketManager socketManager;
    
    private TextView tvGamePhase;
    private TextView tvPot;
    private TextView tvCurrentBet;
    private TextView tvCurrentPlayer;
    
    private CardView[] communityCardViews = new CardView[5];
    private PlayerView[] playerViews = new PlayerView[8];
    
    private Button btnFold;
    private Button btnCall;
    private Button btnRaise;
    private Button btnAllIn;
    private Button btnLeaveGame;
    
    private GameState currentGameState;
    private Player currentPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        
        // 获取传递的数据
        playerName = getIntent().getStringExtra("player_name");
        roomId = getIntent().getStringExtra("room_id");
        
        initViews();
        setupSocketManager();
    }
    
    private void initViews() {
        tvGamePhase = findViewById(R.id.tv_game_phase);
        tvPot = findViewById(R.id.tv_pot);
        tvCurrentBet = findViewById(R.id.tv_current_bet);
        tvCurrentPlayer = findViewById(R.id.tv_current_player);
        
        // 初始化公共牌视图
        communityCardViews[0] = findViewById(R.id.card_community_1);
        communityCardViews[1] = findViewById(R.id.card_community_2);
        communityCardViews[2] = findViewById(R.id.card_community_3);
        communityCardViews[3] = findViewById(R.id.card_community_4);
        communityCardViews[4] = findViewById(R.id.card_community_5);
        
        // 初始化玩家视图
        playerViews[0] = findViewById(R.id.player_1);
        playerViews[1] = findViewById(R.id.player_2);
        playerViews[2] = findViewById(R.id.player_3);
        playerViews[3] = findViewById(R.id.player_4);
        playerViews[4] = findViewById(R.id.player_5);
        playerViews[5] = findViewById(R.id.player_6);
        playerViews[6] = findViewById(R.id.player_7);
        playerViews[7] = findViewById(R.id.player_8);
        
        // 初始化按钮
        btnFold = findViewById(R.id.btn_fold);
        btnCall = findViewById(R.id.btn_call);
        btnRaise = findViewById(R.id.btn_raise);
        btnAllIn = findViewById(R.id.btn_all_in);
        btnLeaveGame = findViewById(R.id.btn_leave_game);
        
        setupButtonListeners();
    }
    
    private void setupButtonListeners() {
        btnFold.setOnClickListener(v -> fold());
        btnCall.setOnClickListener(v -> call());
        btnRaise.setOnClickListener(v -> raise());
        btnAllIn.setOnClickListener(v -> allIn());
        btnLeaveGame.setOnClickListener(v -> leaveGame());
    }
    
    private void setupSocketManager() {
        socketManager = new GameSocketManager(this);
        socketManager.connect();
        
        // 等待连接成功后加入房间
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                runOnUiThread(() -> {
                    if (socketManager.isConnected()) {
                        socketManager.joinRoom(roomId, playerName);
                    } else {
                        Toast.makeText(this, "连接服务器失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void fold() {
        if (socketManager != null) {
            socketManager.fold();
            updateButtonStates(false);
        }
    }
    
    private void call() {
        if (socketManager != null) {
            socketManager.call();
            updateButtonStates(false);
        }
    }
    
    private void raise() {
        // 这里可以实现一个对话框让用户输入加注金额
        if (socketManager != null) {
            socketManager.raise(50); // 示例：加注50
            updateButtonStates(false);
        }
    }
    
    private void allIn() {
        if (socketManager != null) {
            socketManager.allIn();
            updateButtonStates(false);
        }
    }
    
    private void leaveGame() {
        if (socketManager != null) {
            socketManager.disconnect();
        }
        finish();
    }
    
    private void updateButtonStates(boolean enabled) {
        btnFold.setEnabled(enabled);
        btnCall.setEnabled(enabled);
        btnRaise.setEnabled(enabled);
        btnAllIn.setEnabled(enabled);
    }
    
    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "已连接到服务器", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "与服务器断开连接", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onJoinedRoom(boolean success, String message, GameState gameState) {
        runOnUiThread(() -> {
            if (success) {
                updateGameState(gameState);
            }
        });
    }
    
    @Override
    public void onPlayerJoined(Player player, GameState gameState) {
        runOnUiThread(() -> {
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onPlayerLeft(String playerId, GameState gameState) {
        runOnUiThread(() -> {
            Toast.makeText(this, "有玩家离开了游戏", Toast.LENGTH_SHORT).show();
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onGameStarted(GameState gameState) {
        runOnUiThread(() -> {
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onGameStateUpdated(GameState gameState) {
        runOnUiThread(() -> {
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "错误: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateGameState(GameState gameState) {
        if (gameState != null) {
            currentGameState = gameState;
            
            android.util.Log.d("GameActivity", "Updating game state: phase=" + gameState.getGamePhase() + 
                ", pot=" + gameState.getPot() + ", currentBet=" + gameState.getCurrentBet() + 
                ", currentPlayerIndex=" + gameState.getCurrentPlayerIndex() + 
                ", dealerIndex=" + gameState.getDealerIndex());
            
            // 更新游戏信息
            tvGamePhase.setText("游戏阶段: " + gameState.getGamePhaseText());
            tvPot.setText("底池: " + gameState.getPot());
            tvCurrentBet.setText("当前下注: " + gameState.getCurrentBet());
            
            // 更新当前玩家显示
            if (gameState.getCurrentPlayerIndex() >= 0 && gameState.getCurrentPlayerIndex() < gameState.getPlayers().size()) {
                Player currentPlayer = gameState.getPlayers().get(gameState.getCurrentPlayerIndex());
                tvCurrentPlayer.setText("当前玩家: " + currentPlayer.getName());
                android.util.Log.d("GameActivity", "Current player: " + currentPlayer.getName() + 
                    " (index " + gameState.getCurrentPlayerIndex() + ")");
            } else {
                tvCurrentPlayer.setText("当前玩家: 无");
                android.util.Log.d("GameActivity", "No current player - index out of range: " + gameState.getCurrentPlayerIndex());
            }
            
            // 显示庄家信息
            if (gameState.getDealerIndex() >= 0 && gameState.getDealerIndex() < gameState.getPlayers().size()) {
                Player dealer = gameState.getPlayers().get(gameState.getDealerIndex());
                android.util.Log.d("GameActivity", "Dealer: " + dealer.getName() + " (index " + gameState.getDealerIndex() + ")");
            }
            
            // 更新公共牌
            updateCommunityCards(gameState.getCommunityCards());
            
            // 更新玩家视图
            updatePlayerViews(gameState.getPlayers());
            
            // 更新按钮状态
            updateGameButtons(gameState);
        }
    }
    
    private void updateCommunityCards(List<Card> cards) {
        for (int i = 0; i < communityCardViews.length; i++) {
            if (i < cards.size()) {
                Card card = cards.get(i);
                communityCardViews[i].setCard(card);
                communityCardViews[i].setVisibility(View.VISIBLE);
            } else {
                communityCardViews[i].setVisibility(View.INVISIBLE);
            }
        }
    }
    
    private void updatePlayerViews(List<Player> players) {
        android.util.Log.d("GameActivity", "Updating player views with " + players.size() + " players");
        
        for (int i = 0; i < playerViews.length; i++) {
            if (i < players.size()) {
                Player player = players.get(i);
                String currentPlayerId = socketManager.getCurrentPlayerId();
                playerViews[i].setPlayer(player, currentPlayerId);
                playerViews[i].setVisibility(View.VISIBLE);
                
                // 检查是否是当前玩家
                if (player.getName().equals(playerName)) {
                    currentPlayer = player;
                    android.util.Log.d("GameActivity", "Set current player: " + player.getName() + 
                        ", isCurrentPlayer: " + player.isCurrentPlayer() + 
                        ", isDealer: " + player.isDealer());
                }
                
                // 添加调试信息
                android.util.Log.d("GameActivity", "Player " + i + ": " + player.getName() + 
                    ", isCurrentPlayer: " + player.isCurrentPlayer() + 
                    ", isDealer: " + player.isDealer() + 
                    ", chips: " + player.getChips() + 
                    ", bet: " + player.getBet());
            } else {
                playerViews[i].setVisibility(View.INVISIBLE);
            }
        }
    }
    
    private void updateGameButtons(GameState gameState) {
        // 添加调试日志
        android.util.Log.d("GameActivity", "Updating game buttons. Game active: " + gameState.isGameActive());
        android.util.Log.d("GameActivity", "Current player index: " + gameState.getCurrentPlayerIndex());
        android.util.Log.d("GameActivity", "My player name: " + playerName);
        
        // 找到当前玩家（通过名称匹配）
        Player myPlayer = null;
        int myPlayerIndex = -1;
        for (int i = 0; i < gameState.getPlayers().size(); i++) {
            Player p = gameState.getPlayers().get(i);
            if (p.getName().equals(playerName)) {
                myPlayer = p;
                myPlayerIndex = i;
                break;
            }
        }
        
        if (myPlayer != null) {
            android.util.Log.d("GameActivity", "Found my player: " + myPlayer.getName() + 
                " at index " + myPlayerIndex + 
                ", isCurrentPlayer: " + myPlayer.isCurrentPlayer() + 
                ", isDealer: " + myPlayer.isDealer());
        }
        
        if (gameState.isGameActive() && myPlayer != null) {
            // 使用currentPlayerIndex来判断是否轮到当前玩家
            boolean isMyTurn = (myPlayerIndex == gameState.getCurrentPlayerIndex());
            boolean canAct = !myPlayer.isFolded() && !myPlayer.isAllIn();
            
            android.util.Log.d("GameActivity", "Button state: isMyTurn=" + isMyTurn + 
                " (myIndex=" + myPlayerIndex + ", currentIndex=" + gameState.getCurrentPlayerIndex() + 
                "), canAct=" + canAct);
            
            updateButtonStates(isMyTurn && canAct);
            
            if (isMyTurn && canAct) {
                // 根据游戏状态更新按钮文本和可用性
                int callAmount = gameState.getCurrentBet() - myPlayer.getBet();
                if (callAmount < 0) callAmount = 0;
                
                btnCall.setText("跟注 (" + callAmount + ")");
                btnCall.setEnabled(callAmount <= myPlayer.getChips());
                
                btnRaise.setEnabled(myPlayer.getChips() > gameState.getCurrentBet());
                btnAllIn.setEnabled(myPlayer.getChips() > 0);
                
                android.util.Log.d("GameActivity", "Buttons enabled. Call amount: " + callAmount + ", my chips: " + myPlayer.getChips());
            } else {
                android.util.Log.d("GameActivity", "Buttons disabled. isMyTurn: " + isMyTurn + ", canAct: " + canAct);
            }
        } else {
            android.util.Log.d("GameActivity", "Game not active or player not found. Disabling buttons.");
            updateButtonStates(false);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketManager != null) {
            socketManager.disconnect();
        }
    }
} 