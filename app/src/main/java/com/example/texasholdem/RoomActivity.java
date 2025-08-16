package com.example.texasholdem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.texasholdem.adapters.PlayerAdapter;
import com.example.texasholdem.models.GameState;
import com.example.texasholdem.models.Player;
import com.example.texasholdem.network.GameSocketManager;

import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends AppCompatActivity implements GameSocketManager.GameSocketListener {
    
    private String playerName;
    private String roomId;
    private GameSocketManager socketManager;
    
    private TextView tvRoomInfo;
    private TextView tvGamePhase;
    private TextView tvPot;
    private TextView tvConnectionStatus;
    private RecyclerView rvPlayers;
    private Button btnStartGame;
    private Button btnLeaveRoom;
    
    private PlayerAdapter playerAdapter;
    private List<Player> players = new ArrayList<>();
    private GameState currentGameState;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        
        // 获取传递的数据
        playerName = getIntent().getStringExtra("player_name");
        roomId = getIntent().getStringExtra("room_id");
        
        initViews();
        setupRecyclerView();
        setupSocketManager();
    }
    
    private void initViews() {
        tvRoomInfo = findViewById(R.id.tv_room_info);
        tvGamePhase = findViewById(R.id.tv_game_phase);
        tvPot = findViewById(R.id.tv_pot);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        rvPlayers = findViewById(R.id.rv_players);
        btnStartGame = findViewById(R.id.btn_start_game);
        btnLeaveRoom = findViewById(R.id.btn_leave_room);
        
        tvRoomInfo.setText("房间: " + roomId + "\n玩家: " + playerName);
        tvConnectionStatus.setText("连接状态: 连接中...");
        
        btnStartGame.setOnClickListener(v -> startGame());
        btnLeaveRoom.setOnClickListener(v -> leaveRoom());
    }
    
    private void setupRecyclerView() {
        playerAdapter = new PlayerAdapter(players);
        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        rvPlayers.setAdapter(playerAdapter);
    }
    
    private void setupSocketManager() {
        socketManager = new GameSocketManager(this);
        socketManager.connect();
        
        // 等待连接成功后加入房间
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待连接
                runOnUiThread(() -> {
                    if (socketManager.isConnected()) {
                        tvConnectionStatus.setText("连接状态: 已连接");
                        socketManager.joinRoom(roomId, playerName);
                    } else {
                        tvConnectionStatus.setText("连接状态: 连接失败");
                        Toast.makeText(this, "连接服务器失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void startGame() {
        if (socketManager != null) {
            socketManager.startGame();
        }
    }
    
    private void leaveRoom() {
        if (socketManager != null) {
            socketManager.disconnect();
        }
        finish();
    }
    
    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            tvConnectionStatus.setText("连接状态: 已连接");
            Toast.makeText(this, "已连接到服务器", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            tvConnectionStatus.setText("连接状态: 已断开");
            Toast.makeText(this, "与服务器断开连接", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onJoinedRoom(boolean success, String message, GameState gameState) {
        runOnUiThread(() -> {
            if (success) {
                tvConnectionStatus.setText("连接状态: 已加入房间");
                Toast.makeText(this, "成功加入房间", Toast.LENGTH_SHORT).show();
                updateGameState(gameState);
            } else {
                tvConnectionStatus.setText("连接状态: 加入房间失败");
                Toast.makeText(this, "加入房间失败: " + message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    @Override
    public void onPlayerJoined(Player player, GameState gameState) {
        runOnUiThread(() -> {
            if (player != null) {
                Toast.makeText(this, player.getName() + " 加入了房间", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "有玩家加入了房间", Toast.LENGTH_SHORT).show();
            }
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onPlayerLeft(String playerId, GameState gameState) {
        runOnUiThread(() -> {
            Toast.makeText(this, "有玩家离开了房间", Toast.LENGTH_SHORT).show();
            updateGameState(gameState);
        });
    }
    
    @Override
    public void onGameStarted(GameState gameState) {
        runOnUiThread(() -> {
            Toast.makeText(this, "游戏开始！", Toast.LENGTH_SHORT).show();
            updateGameState(gameState);
            
            // 跳转到游戏界面
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("player_name", playerName);
            intent.putExtra("room_id", roomId);
            startActivity(intent);
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
            tvConnectionStatus.setText("连接状态: 错误");
            Toast.makeText(this, "错误: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateGameState(GameState gameState) {
        if (gameState != null) {
            currentGameState = gameState;
            
            // 添加调试日志
            android.util.Log.d("RoomActivity", "Updating game state with " + 
                (gameState.getPlayers() != null ? gameState.getPlayers().size() : 0) + " players");
            
            // 更新游戏阶段
            tvGamePhase.setText("游戏阶段: " + gameState.getGamePhaseText());
            
            // 更新底池
            tvPot.setText("底池: " + gameState.getPot());
            
            // 更新玩家列表
            if (gameState.getPlayers() != null) {
                android.util.Log.d("RoomActivity", "Clearing and updating player list");
                players.clear();
                players.addAll(gameState.getPlayers());
                
                android.util.Log.d("RoomActivity", "Player list now contains " + players.size() + " players");
                for (Player p : players) {
                    android.util.Log.d("RoomActivity", "Player: " + p.getName() + " (ID: " + p.getId() + ")");
                }
                
                // 设置当前玩家ID
                String currentPlayerId = socketManager.getCurrentPlayerId();
                playerAdapter.setCurrentPlayerId(currentPlayerId);
                
                playerAdapter.notifyDataSetChanged();
                
                // 更新开始游戏按钮状态
                btnStartGame.setEnabled(gameState.getPlayers().size() >= 2 && 
                                     "waiting".equals(gameState.getGamePhase()));
            }
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