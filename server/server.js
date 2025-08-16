const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

app.use(cors());
app.use(express.json());

// 游戏状态管理
class Game {
  constructor(roomId) {
    this.roomId = roomId;
    this.players = [];
    this.deck = [];
    this.communityCards = [];
    this.pot = 0;
    this.currentBet = 0;
    this.dealerIndex = 0;
    this.currentPlayerIndex = 0;
    this.gamePhase = 'waiting'; // waiting, preflop, flop, turn, river, showdown
    this.minPlayers = 2;
    this.maxPlayers = 8;
  }

  addPlayer(player) {
    console.log(`Attempting to add player: ${player.name} (${player.socketId})`);
    console.log(`Current players in room: ${this.players.length}`);
    
    if (this.players.length < this.maxPlayers) {
      // 检查玩家是否已经存在（按名称检查，因为可能是同一个用户重新连接）
      const existingPlayerByName = this.players.find(p => p.name === player.name);
      if (existingPlayerByName) {
        console.log(`Player with name '${player.name}' already exists. Updating socketId.`);
        // 更新现有玩家的socketId（处理重连情况）
        existingPlayerByName.socketId = player.socketId;
        return true;
      }
      
      // 检查socketId是否已存在
      const existingPlayerBySocket = this.players.find(p => p.socketId === player.socketId);
      if (existingPlayerBySocket) {
        console.log(`Player with socketId '${player.socketId}' already exists.`);
        return false;
      }
      
      this.players.push({
        id: player.id,
        socketId: player.socketId,
        name: player.name,
        chips: 1000,
        hand: [],
        bet: 0,
        folded: false,
        allIn: false
      });
      
      console.log(`Successfully added player: ${player.name}. Total players: ${this.players.length}`);
      console.log(`Players in room:`, this.players.map(p => ({ name: p.name, socketId: p.socketId })));
      
      return true;
    }
    
    console.log(`Room is full. Cannot add player: ${player.name}`);
    return false;
  }

  removePlayer(socketId) {
    const index = this.players.findIndex(p => p.socketId === socketId);
    if (index !== -1) {
      this.players.splice(index, 1);
      return true;
    }
    return false;
  }

  getPlayerBySocketId(socketId) {
    return this.players.find(p => p.socketId === socketId);
  }

  startGame() {
    if (this.players.length >= this.minPlayers) {
      console.log(`Starting game in room ${this.roomId} with ${this.players.length} players`);
      
      // 重置游戏状态
      this.gamePhase = 'preflop';
      this.pot = 0;
      this.currentBet = 0;
      this.communityCards = [];
      
      // 随机设置庄家
      this.dealerIndex = Math.floor(Math.random() * this.players.length);
      
      // 发牌
      this.dealCards();
      
      // 开始下注轮次（不自动设置盲注，让庄家自定义）
      this.bettingRound();
      
      console.log(`Game started successfully. Final state:`);
      console.log(`- Random Dealer: ${this.players[this.dealerIndex].name} (index ${this.dealerIndex})`);
      console.log(`- Current player: ${this.players[this.currentPlayerIndex].name} (index ${this.currentPlayerIndex})`);
      console.log(`- Game phase: ${this.gamePhase}`);
      console.log(`- Pot: ${this.pot}`);
      console.log(`- Current bet: ${this.currentBet}`);
    }
  }

  dealCards() {
    this.deck = this.createDeck();
    this.shuffleDeck();
    
    // 给每个玩家发两张牌
    this.players.forEach(player => {
      player.hand = [this.deck.pop(), this.deck.pop()];
      player.bet = 0;
      player.folded = false;
      player.allIn = false;
    });
  }

  createDeck() {
    const suits = ['hearts', 'diamonds', 'clubs', 'spades'];
    const ranks = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A'];
    const deck = [];
    
    for (let suit of suits) {
      for (let rank of ranks) {
        deck.push({ suit, rank, value: this.getCardValue(rank) });
      }
    }
    return deck;
  }

  getCardValue(rank) {
    const values = {
      '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9, '10': 10,
      'J': 11, 'Q': 12, 'K': 13, 'A': 14
    };
    return values[rank];
  }

  shuffleDeck() {
    for (let i = this.deck.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [this.deck[i], this.deck[j]] = [this.deck[j], this.deck[i]];
    }
  }

  bettingRound() {
    // 设置当前玩家为庄家（庄家先下注）
    this.currentPlayerIndex = this.dealerIndex;
    this.currentBet = 0;
    
    console.log(`Starting betting round. Dealer index: ${this.dealerIndex}, Current player index: ${this.currentPlayerIndex}`);
    console.log(`Current player (Dealer): ${this.players[this.currentPlayerIndex].name}`);
    console.log(`Base bet is 10. Dealer can set custom first bet.`);
    
    // 不自动设置盲注，让庄家自定义第一个注
    // 底注是10，庄家可以在此基础上增加
  }

  nextPhase() {
    switch (this.gamePhase) {
      case 'preflop':
        this.gamePhase = 'flop';
        this.dealCommunityCards(3);
        break;
      case 'flop':
        this.gamePhase = 'turn';
        this.dealCommunityCards(1);
        break;
      case 'turn':
        this.gamePhase = 'river';
        this.dealCommunityCards(1);
        break;
      case 'river':
        this.gamePhase = 'showdown';
        this.showdown();
        break;
    }
    
    if (this.gamePhase !== 'showdown') {
      // 每轮都从庄家开始下注
      this.bettingRound();
    }
  }

  dealCommunityCards(count) {
    for (let i = 0; i < count; i++) {
      this.communityCards.push(this.deck.pop());
    }
  }

  showdown() {
    // 计算每个玩家的牌型并确定赢家
    const results = this.players
      .filter(p => !p.folded)
      .map(player => ({
        player,
        handRank: this.evaluateHand(player.hand, this.communityCards)
      }))
      .sort((a, b) => b.handRank - a.handRank);

    if (results.length > 0) {
      const winner = results[0];
      winner.player.chips += this.pot;
      this.pot = 0;
    }
  }

  evaluateHand(holeCards, communityCards) {
    // 简化的牌型评估
    const allCards = [...holeCards, ...communityCards];
    return this.getHandRank(allCards);
  }

  getHandRank(cards) {
    // 简化的牌型判断逻辑
    const values = cards.map(c => c.value).sort((a, b) => b - a);
    const suits = cards.map(c => c.suit);
    
    // 检查同花
    const suitCounts = {};
    suits.forEach(suit => suitCounts[suit] = (suitCounts[suit] || 0) + 1);
    const isFlush = Object.values(suitCounts).some(count => count >= 5);
    
    // 检查顺子
    let isStraight = false;
    for (let i = 0; i <= values.length - 5; i++) {
      if (values[i] - values[i + 4] === 4) {
        isStraight = true;
        break;
      }
    }
    
    // 检查对子、三条、四条
    const valueCounts = {};
    values.forEach(value => valueCounts[value] = (valueCounts[value] || 0) + 1);
    const counts = Object.values(valueCounts).sort((a, b) => b - a);
    
    if (isFlush && isStraight) return 8000; // 同花顺
    if (counts[0] === 4) return 7000; // 四条
    if (counts[0] === 3 && counts[1] === 2) return 6000; // 葫芦
    if (isFlush) return 5000; // 同花
    if (isStraight) return 4000; // 顺子
    if (counts[0] === 3) return 3000; // 三条
    if (counts[0] === 2 && counts[1] === 2) return 2000; // 两对
    if (counts[0] === 2) return 1000; // 一对
    
    return values[0]; // 高牌
  }

  getGameState() {
    return {
      roomId: this.roomId,
      players: this.players.map(p => ({
        id: p.id,
        name: p.name,
        chips: p.chips,
        bet: p.bet,
        folded: p.folded,
        allIn: p.allIn,
        hand: p.hand
      })),
      communityCards: this.communityCards,
      pot: this.pot,
      currentBet: this.currentBet,
      currentPlayerIndex: this.currentPlayerIndex,
      gamePhase: this.gamePhase,
      dealerIndex: this.dealerIndex
    };
  }

  // 为特定玩家获取游戏状态（隐藏其他玩家的手牌）
  getGameStateForPlayer(playerSocketId) {
    console.log(`Getting game state for player with socketId: ${playerSocketId}`);
    console.log(`Total players in room: ${this.players.length}`);
    console.log(`Players:`, this.players.map(p => ({ id: p.id, name: p.name, socketId: p.socketId })));
    console.log(`Current player index: ${this.currentPlayerIndex}, Dealer index: ${this.dealerIndex}`);
    
    const gameState = {
      roomId: this.roomId,
      players: this.players.map((p, index) => {
        const isCurrentPlayer = index === this.currentPlayerIndex;
        const isDealer = index === this.dealerIndex;
        
        console.log(`Player ${p.name} (index ${index}): isCurrentPlayer=${isCurrentPlayer}, isDealer=${isDealer}`);
        
        return {
          id: p.id,
          name: p.name,
          chips: p.chips,
          bet: p.bet,
          folded: p.folded,
          allIn: p.allIn,
          // 只显示自己的手牌，其他玩家显示空数组
          hand: p.socketId === playerSocketId ? p.hand : [],
          // 添加当前玩家标识
          isCurrentPlayer: isCurrentPlayer,
          isDealer: isDealer
        };
      }),
      communityCards: this.communityCards,
      pot: this.pot,
      currentBet: this.currentBet,
      currentPlayerIndex: this.currentPlayerIndex,
      gamePhase: this.gamePhase,
      dealerIndex: this.dealerIndex,
      // 添加游戏状态标识
      isGameActive: this.gamePhase !== 'waiting',
      canStartGame: this.players.length >= this.minPlayers && this.gamePhase === 'waiting',
      // 添加底注信息
      baseBet: 10
    };
    
    console.log(`Returning game state with ${gameState.players.length} players`);
    console.log(`Current player index: ${this.currentPlayerIndex}, Game phase: ${this.gamePhase}`);
    console.log(`Base bet: ${gameState.baseBet}, Current bet: ${gameState.currentBet}`);
    return gameState;
  }
}

// 房间管理
const rooms = new Map();
// 玩家连接映射
const playerConnections = new Map(); // socketId -> { roomId, playerName }

// Socket.IO 连接处理
io.on('connection', (socket) => {
  console.log('User connected:', socket.id);

  socket.on('joinRoom', (data) => {
    const { roomId, playerName } = data;
    
    console.log(`Player ${playerName} (${socket.id}) attempting to join room ${roomId}`);
    
    if (!rooms.has(roomId)) {
      rooms.set(roomId, new Game(roomId));
      console.log(`Created new room: ${roomId}`);
    }
    
    const game = rooms.get(roomId);
    
    // 检查玩家是否已经在该房间中
    const existingPlayer = game.players.find(p => p.name === playerName);
    if (existingPlayer) {
      console.log(`Player ${playerName} already in room ${roomId}. Updating connection.`);
      
      // 更新现有玩家的socketId
      existingPlayer.socketId = socket.id;
      
      // 更新玩家连接映射
      playerConnections.set(socket.id, { roomId, playerName });
      
      // 加入房间
      socket.join(roomId);
      
      // 发送成功消息给重新连接的玩家
      socket.emit('joinedRoom', { 
        success: true, 
        gameState: game.getGameStateForPlayer(socket.id),
        playerId: existingPlayer.id,
        currentPlayerId: existingPlayer.id
      });
      
      console.log(`Player ${playerName} reconnected to room ${roomId}`);
      return;
    }
    
    // 生成唯一玩家ID
    const playerId = `player_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const player = { 
      id: playerId, 
      socketId: socket.id, 
      name: playerName 
    };
    
    if (game.addPlayer(player)) {
      // 记录玩家连接信息
      playerConnections.set(socket.id, { roomId, playerName });
      
      // 加入房间
      socket.join(roomId);
      
      // 发送成功消息给新玩家
      socket.emit('joinedRoom', { 
        success: true, 
        gameState: game.getGameStateForPlayer(socket.id),
        playerId: playerId,
        currentPlayerId: playerId
      });
      
      // 通知房间内所有其他玩家有新玩家加入
      socket.to(roomId).emit('playerJoined', { 
        player: game.players.find(p => p.socketId === socket.id), 
        gameState: game.getGameStateForPlayer(socket.id)
      });
      
      // 不需要额外发送gameStateUpdated，因为joinedRoom和playerJoined已经包含了最新的游戏状态
      console.log(`Player ${playerName} (${playerId}) successfully joined room ${roomId}`);
      console.log(`Room ${roomId} now has ${game.players.length} players`);
    } else {
      socket.emit('joinedRoom', { 
        success: false, 
        message: 'Room is full or player already exists' 
      });
      console.log(`Player ${playerName} failed to join room ${roomId}`);
    }
  });

  socket.on('startGame', (data) => {
    const { roomId } = data;
    const game = rooms.get(roomId);
    
    console.log(`Starting game request for room ${roomId}`);
    
    if (game && game.players.length >= game.minPlayers) {
      game.startGame();
      
      console.log(`Game started successfully. Broadcasting to all players.`);
      
      // 为每个玩家发送个性化的游戏状态
      game.players.forEach(p => {
        const playerSocket = io.sockets.sockets.get(p.socketId);
        if (playerSocket) {
          const gameState = game.getGameStateForPlayer(p.socketId);
          console.log(`Sending gameStarted to player ${p.name} with ${gameState.players.length} players`);
          
          playerSocket.emit('gameStarted', { 
            gameState: gameState,
            currentPlayerId: p.id
          });
        }
      });
      
      console.log(`Game started in room ${roomId}`);
    } else {
      console.log(`Cannot start game: players=${game?.players.length}, minPlayers=${game?.minPlayers}`);
    }
  });

  socket.on('makeBet', (data) => {
    const { roomId, action, amount } = data;
    const game = rooms.get(roomId);
    
    console.log(`Player ${socket.id} making bet: ${action} ${amount || ''} in room ${roomId}`);
    
    if (game && game.gamePhase !== 'waiting') {
      const player = game.getPlayerBySocketId(socket.id);
      if (player && !player.folded && !player.allIn) {
        console.log(`Processing bet for player: ${player.name}, action: ${action}`);
        
        switch (action) {
          case 'fold':
            player.folded = true;
            console.log(`Player ${player.name} folded`);
            break;
          case 'call':
            const callAmount = game.currentBet - player.bet;
            if (callAmount <= player.chips) {
              player.chips -= callAmount;
              player.bet += callAmount;
              game.pot += callAmount;
              console.log(`Player ${player.name} called ${callAmount}`);
            }
            break;
          case 'raise':
            if (amount && amount >= 10 && amount <= player.chips) { // 确保下注至少是底注10
              player.chips -= amount;
              player.bet += amount;
              game.pot += amount;
              game.currentBet = player.bet;
              console.log(`Player ${player.name} raised to ${amount}`);
            } else {
              console.log(`Invalid raise amount: ${amount}. Must be >= 10 and <= chips (${player.chips})`);
              return; // 无效下注，不继续处理
            }
            break;
          case 'allIn':
            player.allIn = true;
            game.pot += player.chips;
            player.bet += player.chips;
            player.chips = 0;
            if (player.bet > game.currentBet) {
              game.currentBet = player.bet;
            }
            console.log(`Player ${player.name} went all in with ${player.bet}`);
            break;
        }
        
        // 移动到下一个玩家
        do {
          game.currentPlayerIndex = (game.currentPlayerIndex + 1) % game.players.length;
        } while (game.players[game.currentPlayerIndex].folded || game.players[game.currentPlayerIndex].allIn);
        
        console.log(`Next player index: ${game.currentPlayerIndex}, player: ${game.players[game.currentPlayerIndex].name}`);
        
        // 检查是否所有玩家都行动完毕
        const activePlayers = game.players.filter(p => !p.folded && !p.allIn);
        if (activePlayers.length === 1 || allBetsEqual(game)) {
          console.log(`Betting round complete, moving to next phase`);
          game.nextPhase();
        }
        
        // 为每个玩家发送个性化的游戏状态
        game.players.forEach(p => {
          const playerSocket = io.sockets.sockets.get(p.socketId);
          if (playerSocket) {
            playerSocket.emit('gameStateUpdated', { 
              gameState: game.getGameStateForPlayer(p.socketId),
              currentPlayerId: p.id
            });
          }
        });
      } else {
        console.log(`Player ${socket.id} cannot make bet: folded=${player?.folded}, allIn=${player?.allIn}`);
      }
    } else {
      console.log(`Game not active or in waiting phase`);
    }
  });

  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
    
    // 获取玩家连接信息
    const connectionInfo = playerConnections.get(socket.id);
    if (connectionInfo) {
      const { roomId } = connectionInfo;
      const game = rooms.get(roomId);
      
      if (game) {
        // 从房间中移除玩家
        if (game.removePlayer(socket.id)) {
          console.log(`Player ${connectionInfo.playerName} left room ${roomId}`);
          
          // 通知房间内其他玩家
          const remainingPlayers = game.players.filter(p => p.socketId !== socket.id);
          remainingPlayers.forEach(p => {
            const playerSocket = io.sockets.sockets.get(p.socketId);
            if (playerSocket) {
              playerSocket.emit('playerLeft', { 
                playerId: socket.id, 
                gameState: game.getGameStateForPlayer(p.socketId)
              });
            }
          });
          
          // 如果房间空了，删除房间
          if (game.players.length === 0) {
            rooms.delete(roomId);
            console.log(`Room ${roomId} deleted (empty)`);
          } else {
            console.log(`Room ${roomId} now has ${game.players.length} players`);
          }
        }
      }
      
      // 清理连接信息
      playerConnections.delete(socket.id);
    }
    
    // 清理所有房间中可能存在的无效socketId
    rooms.forEach((game, roomId) => {
      const invalidPlayers = game.players.filter(p => !io.sockets.sockets.has(p.socketId));
      if (invalidPlayers.length > 0) {
        console.log(`Cleaning up ${invalidPlayers.length} invalid players in room ${roomId}`);
        invalidPlayers.forEach(p => {
          game.removePlayer(p.socketId);
          console.log(`Removed invalid player: ${p.name} (${p.socketId})`);
        });
        
        // 通知房间内其他玩家
        game.players.forEach(p => {
          const playerSocket = io.sockets.sockets.get(p.socketId);
          if (playerSocket) {
            playerSocket.emit('gameStateUpdated', { 
              gameState: game.getGameStateForPlayer(p.socketId),
              currentPlayerId: p.id
            });
          }
        });
      }
    });
  });
});

// 辅助函数
function allBetsEqual(game) {
  const activePlayers = game.players.filter(p => !p.folded && !p.allIn);
  if (activePlayers.length === 0) return true;
  
  const firstBet = activePlayers[0].bet;
  return activePlayers.every(p => p.bet === firstBet || p.allIn);
}

// API 路由
app.get('/api/rooms', (req, res) => {
  const roomList = Array.from(rooms.keys()).map(roomId => ({
    roomId,
    playerCount: rooms.get(roomId).players.length,
    maxPlayers: rooms.get(roomId).maxPlayers,
    gamePhase: rooms.get(roomId).gamePhase
  }));
  res.json(roomList);
});

app.get('/api/rooms/:roomId', (req, res) => {
  const { roomId } = req.params;
  const game = rooms.get(roomId);
  
  if (game) {
    res.json(game.getGameState());
  } else {
    res.status(404).json({ error: 'Room not found' });
  }
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Texas Hold'em Server running on port ${PORT}`);
  console.log(`Server URL: http://localhost:${PORT}`);
  
  // 定期清理无效连接
  setInterval(() => {
    console.log('Running periodic cleanup...');
    rooms.forEach((game, roomId) => {
      const invalidPlayers = game.players.filter(p => !io.sockets.sockets.has(p.socketId));
      if (invalidPlayers.length > 0) {
        console.log(`Periodic cleanup: removing ${invalidPlayers.length} invalid players in room ${roomId}`);
        invalidPlayers.forEach(p => {
          game.removePlayer(p.socketId);
          console.log(`Periodic cleanup: removed invalid player: ${p.name} (${p.socketId})`);
        });
        
        // 通知房间内其他玩家
        game.players.forEach(p => {
          const playerSocket = io.sockets.sockets.get(p.socketId);
          if (playerSocket) {
            playerSocket.emit('gameStateUpdated', { 
              gameState: game.getGameStateForPlayer(p.socketId),
              currentPlayerId: p.id
            });
          }
        });
      }
    });
  }, 30000); // 每30秒清理一次
}); 