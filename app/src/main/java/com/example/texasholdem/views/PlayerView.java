package com.example.texasholdem.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.texasholdem.R;
import com.example.texasholdem.models.Card;
import com.example.texasholdem.models.Player;

import java.util.List;

public class PlayerView extends LinearLayout {
    
    private TextView tvPlayerName;
    private TextView tvChips;
    private TextView tvBet;
    private TextView tvStatus;
    private LinearLayout llCards;
    private CardView[] cardViews = new CardView[2];
    
    public PlayerView(Context context) {
        super(context);
        init();
    }
    
    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);
        
        // 加载布局
        LayoutInflater.from(getContext()).inflate(R.layout.view_player, this, true);
        
        // 初始化视图
        tvPlayerName = findViewById(R.id.tv_player_name);
        tvChips = findViewById(R.id.tv_chips);
        tvBet = findViewById(R.id.tv_bet);
        tvStatus = findViewById(R.id.tv_status);
        llCards = findViewById(R.id.ll_cards);
        
        // 初始化卡牌视图
        cardViews[0] = findViewById(R.id.card_1);
        cardViews[1] = findViewById(R.id.card_2);
    }
    
    public void setPlayer(Player player, String currentPlayerId) {
        if (player == null) {
            setVisibility(GONE);
            return;
        }
        
        setVisibility(VISIBLE);
        
        // 检查是否是当前玩家
        boolean isCurrentPlayer = currentPlayerId != null && currentPlayerId.equals(player.getId());
        
        // 设置玩家信息
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
        
        // 设置卡牌
        setPlayerCards(player.getHand(), isCurrentPlayer);
        
        // 如果是当前玩家，设置特殊背景
        if (isCurrentPlayer) {
            setBackgroundColor(0xFFE8F5E8); // 浅绿色背景
        } else {
            setBackgroundColor(0xFFFFFFFF); // 白色背景
        }
    }
    
    private void setPlayerCards(List<Card> cards, boolean isCurrentPlayer) {
        if (cards == null || cards.isEmpty()) {
            // 隐藏卡牌
            for (CardView cardView : cardViews) {
                cardView.setVisibility(GONE);
            }
            return;
        }
        
        // 显示卡牌
        for (int i = 0; i < cardViews.length; i++) {
            if (i < cards.size()) {
                Card card = cards.get(i);
                cardViews[i].setCard(card);
                cardViews[i].setVisibility(VISIBLE);
                
                // 只有当前玩家能看到自己的手牌，其他玩家看到背面
                card.setVisible(isCurrentPlayer);
            } else {
                cardViews[i].setVisibility(GONE);
            }
        }
    }
    
    public void setDealer(boolean isDealer) {
        if (isDealer) {
            tvPlayerName.setText(tvPlayerName.getText() + " (庄家)");
        }
    }
} 