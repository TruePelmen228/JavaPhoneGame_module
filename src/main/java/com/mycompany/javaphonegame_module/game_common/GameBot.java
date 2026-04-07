package com.mycompany.javaphonegame_module.game_common;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameBot {
    private String botId;
    private String botName;
    private WebRTCDataChannel channel;
    private GameMessageHandler handler;
    private String currentGameType;
    private Random random = new Random();
    private Gson gson = new Gson();
    private String[][] board = new String[3][3];
    
    public GameBot(String botId, String botName, GameMessageHandler handler) {
        this.botId = botId;
        this.botName = botName;
        this.handler = handler;
        
        // Инициализируем доску
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
        
        this.channel = new WebRTCDataChannel("bot_channel");
        handler.registerPlayerChannel(botId, channel);
        
        channel.setMessageHandler(message -> {
            System.out.println("\n🤖 [БОТ] Получено сообщение: " + message);
            handleIncomingMessage(message);
        });
        
        System.out.println("🤖 Бот " + botName + " (" + botId + ") создан");
    }
    
    private void handleIncomingMessage(String message) {
        try {
            Map<String, Object> msgMap = gson.fromJson(message, Map.class);
            String type = (String) msgMap.get("type");
            
            if ("handshake".equals(type)) {
                String gameType = (String) msgMap.get("game");
                System.out.println("🤖 [БОТ] Получен handshake");
                sendHandshakeResponse(gameType);
            }
            else if ("move".equals(type)) {
                // Обновляем доску ходом игрока
                Map<String, Object> coords = (Map<String, Object>) msgMap.get("coordinates");
                int row = ((Double) coords.get("row")).intValue();
                int col = ((Double) coords.get("col")).intValue();
                board[row][col] = "X";
                System.out.println("🤖 [БОТ] Обновлена доска: игрок походил на [" + row + "," + col + "]");
                
                // Отвечаем валидным ходом
                sendValidMove();
            }
            else if ("move_response".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) msgMap.get("payload");
                if (payload != null && "accepted".equals(payload.get("status"))) {
                    // Наш ход принят, обновляем доску
                    System.out.println("🤖 [БОТ] Мой ход принят");
                }
            }
        } catch (Exception e) {
            System.err.println("🤖 [БОТ] Ошибка: " + e.getMessage());
        }
    }
    
    private void sendValidMove() {
        // Собираем все свободные клетки
        List<int[]> freeCells = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    freeCells.add(new int[]{i, j});
                }
            }
        }
        
        if (freeCells.isEmpty()) {
            System.out.println("🤖 [БОТ] Нет свободных клеток!");
            return;
        }
        
        // Выбираем случайную свободную клетку
        int[] move = freeCells.get(random.nextInt(freeCells.size()));
        int row = move[0];
        int col = move[1];
        
        System.out.println("🤖 [БОТ] Выбрана свободная клетка: [" + row + "," + col + "]");
        System.out.println("🤖 [БОТ] Всего свободных клеток: " + freeCells.size());
        
        // Обновляем свою доску
        board[row][col] = "O";
        
        // Отправляем ход
        Map<String, Object> moveMsg = new HashMap<>();
        moveMsg.put("type", "move");
        moveMsg.put("game", currentGameType);
        moveMsg.put("moveId", String.valueOf(System.currentTimeMillis()));
        
        Map<String, Object> coords = new HashMap<>();
        coords.put("row", row);
        coords.put("col", col);
        moveMsg.put("coordinates", coords);
        
        String json = gson.toJson(moveMsg);
        channel.send(json);
        handler.handleBotMessage(botId, json);
        
        System.out.println("🤖 [БОТ] Отправил ход: [" + row + "," + col + "]");
    }
    
    private void sendHandshakeResponse(String gameType) {
        this.currentGameType = gameType;
        
        // Очищаем доску
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
        
        Map<String, Object> handshake = new HashMap<>();
        handshake.put("type", "handshake");
        handshake.put("game", gameType);
        handshake.put("playerName", botName);
        
        String json = gson.toJson(handshake);
        System.out.println("🤖 [БОТ] Отправляю ответный handshake");
        
        channel.send(json);
        handler.handleBotMessage(botId, json);
    }
    
    public void startGame(String gameType) {
        this.currentGameType = gameType;
        // Очищаем доску при старте
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
        System.out.println("🤖 [БОТ] Готов к игре " + gameType);
    }
    
    public String getBotId() { return botId; }
    public String getBotName() { return botName; }
}