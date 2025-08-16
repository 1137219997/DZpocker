package com.example.texasholdem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.texasholdem.R;
import com.example.texasholdem.models.Player;

import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
    
    private List<Player> players;
    private String currentPlayerId;
    
    public PlayerAdapter(List<Player> players) {
        this.players = players;
    }
    
    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.bind(player, currentPlayerId);
    }
    
    @Override
    public int getItemCount() {
        return players.size();
    }
    
    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlayerName;
        private TextView tvChips;
        private TextView tvBet;
        private TextView tvStatus;
        
        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tv_player_name);
            tvChips = itemView.findViewById(R.id.tv_chips);
            tvBet = itemView.findViewById(R.id.tv_bet);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
        
        public void bind(Player player, String currentPlayerId) {
            // 检查是否是当前玩家
            boolean isCurrentPlayer = currentPlayerId != null && currentPlayerId.equals(player.getId());
            
            tvPlayerName.setText(player.getName() + (isCurrentPlayer ? " (我)" : ""));
            tvChips.setText("筹码: " + player.getChips());
            tvBet.setText("下注: " + player.getBet());
            tvStatus.setText(player.getStatusText());
            
            // 根据玩家状态设置不同的颜色
            if (player.isFolded()) {
                tvStatus.setTextColor(0xFFE74C3C); // 红色
            } else if (player.isAllIn()) {
                tvStatus.setTextColor(0xFFF39C12); // 橙色
            } else if (player.getBet() > 0) {
                tvStatus.setTextColor(0xFF27AE60); // 绿色
            } else {
                tvStatus.setTextColor(0xFF2C3E50); // 深色
            }
            
            // 如果是当前玩家，设置特殊背景
            if (isCurrentPlayer) {
                itemView.setBackgroundColor(0xFFE8F5E8); // 浅绿色背景
            } else {
                itemView.setBackgroundColor(0xFFFFFFFF); // 白色背景
            }
        }
    }
} 