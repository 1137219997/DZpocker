package com.example.texasholdem.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.texasholdem.models.Card;

public class CardView extends View {
    
    private Card card;
    private Paint paint;
    private RectF cardRect;
    
    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 120;
    private static final int CORNER_RADIUS = 8;
    
    public CardView(Context context) {
        super(context);
        init();
    }
    
    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardRect = new RectF();
        
        // 设置默认尺寸
        setMinimumWidth(CARD_WIDTH);
        setMinimumHeight(CARD_HEIGHT);
    }
    
    public void setCard(Card card) {
        this.card = card;
        invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(CARD_WIDTH, widthMeasureSpec);
        int height = resolveSize(CARD_HEIGHT, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (card == null) {
            drawEmptyCard(canvas);
            return;
        }
        
        drawCard(canvas);
    }
    
    private void drawEmptyCard(Canvas canvas) {
        // 绘制空白卡牌背景
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.FILL);
        
        cardRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, paint);
        
        // 绘制边框
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, paint);
    }
    
    private void drawCard(Canvas canvas) {
        // 绘制卡牌背景
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        
        cardRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, paint);
        
        // 绘制边框
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, paint);
        
        if (card.isVisible()) {
            // 绘制卡牌内容
            drawCardContent(canvas);
        } else {
            // 绘制卡牌背面
            drawCardBack(canvas);
        }
    }
    
    private void drawCardContent(Canvas canvas) {
        // 绘制花色符号
        paint.setColor(card.getSuitColor());
        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.CENTER);
        
        String suitSymbol = card.getSuitSymbol();
        canvas.drawText(suitSymbol, getWidth() / 2f, 30, paint);
        
        // 绘制点数
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);
        canvas.drawText(card.getRank(), getWidth() / 2f, 60, paint);
        
        // 绘制右下角的小符号
        paint.setTextSize(16);
        paint.setColor(card.getSuitColor());
        canvas.drawText(suitSymbol, getWidth() - 15, getHeight() - 10, paint);
        
        paint.setColor(Color.BLACK);
        canvas.drawText(card.getRank(), getWidth() - 15, getHeight() - 25, paint);
    }
    
    private void drawCardBack(Canvas canvas) {
        // 绘制卡牌背面图案
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        
        // 绘制网格图案
        paint.setStrokeWidth(1);
        for (int i = 0; i < getWidth(); i += 10) {
            canvas.drawLine(i, 0, i, getHeight(), paint);
        }
        for (int i = 0; i < getHeight(); i += 10) {
            canvas.drawLine(0, i, getWidth(), i, paint);
        }
        
        // 绘制中心文字
        paint.setColor(Color.WHITE);
        paint.setTextSize(16);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("POKER", getWidth() / 2f, getHeight() / 2f, paint);
    }
} 