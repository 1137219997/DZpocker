package com.example.texasholdem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private EditText etPlayerName;
    private EditText etRoomId;
    private Button btnJoinRoom;
    private Button btnCreateRoom;
    private TextView tvRoomIdDisplay;
    private Button btnCopyRoomId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etPlayerName = findViewById(R.id.et_player_name);
        etRoomId = findViewById(R.id.et_room_id);
        btnJoinRoom = findViewById(R.id.btn_join_room);
        btnCreateRoom = findViewById(R.id.btn_create_room);
        tvRoomIdDisplay = findViewById(R.id.tv_room_id_display);
        btnCopyRoomId = findViewById(R.id.btn_copy_room_id);
        
        // 初始隐藏房间ID显示
        tvRoomIdDisplay.setVisibility(View.GONE);
        btnCopyRoomId.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        btnJoinRoom.setOnClickListener(v -> joinRoom());
        btnCreateRoom.setOnClickListener(v -> createRoom());
        btnCopyRoomId.setOnClickListener(v -> copyRoomId());
    }
    
    private void joinRoom() {
        String playerName = etPlayerName.getText().toString().trim();
        String roomId = etRoomId.getText().toString().trim();
        
        if (playerName.isEmpty()) {
            Toast.makeText(this, "请输入玩家名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (roomId.isEmpty()) {
            Toast.makeText(this, "请输入房间ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra("player_name", playerName);
        intent.putExtra("room_id", roomId);
        startActivity(intent);
    }
    
    private void createRoom() {
        String playerName = etPlayerName.getText().toString().trim();
        
        if (playerName.isEmpty()) {
            Toast.makeText(this, "请输入玩家名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 生成简单的房间ID，便于测试
        String roomId = "room_" + System.currentTimeMillis() % 10000;
        
        // 显示房间ID
        tvRoomIdDisplay.setText("房间ID: " + roomId);
        tvRoomIdDisplay.setVisibility(View.VISIBLE);
        btnCopyRoomId.setVisibility(View.VISIBLE);
        
        // 自动填入房间ID输入框
        etRoomId.setText(roomId);
        
        Toast.makeText(this, "房间创建成功！房间ID: " + roomId, Toast.LENGTH_LONG).show();
        
        // 自动跳转到房间
        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra("player_name", playerName);
        intent.putExtra("room_id", roomId);
        startActivity(intent);
    }
    
    private void copyRoomId() {
        String roomId = etRoomId.getText().toString().trim();
        if (!roomId.isEmpty()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("房间ID", roomId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "房间ID已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }
} 