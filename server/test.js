// 测试脚本：验证服务器端数据格式
const Game = require('./server.js').Game;

// 创建测试游戏实例
const game = new Game('test_room');

// 模拟添加玩家
const player1 = { 
  id: 'player_1', 
  socketId: 'socket_1', 
  name: 'Player1' 
};

const player2 = { 
  id: 'player_2', 
  socketId: 'socket_2', 
  name: 'Player2' 
};

game.addPlayer(player1);
game.addPlayer(player2);

// 获取游戏状态
const gameState = game.getGameState();
console.log('Game State:');
console.log(JSON.stringify(gameState, null, 2));

// 测试玩家对象
const testPlayer = game.players.find(p => p.socketId === 'socket_1');
console.log('\nTest Player Object:');
console.log(JSON.stringify(testPlayer, null, 2)); 