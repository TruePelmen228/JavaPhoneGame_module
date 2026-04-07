package com.mycompany.javaphonegame_module.game_common;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameMessageHandler {
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> playerToSession = new ConcurrentHashMap<>();
    private final Map<String, WebRTCDataChannel> playerChannels = new ConcurrentHashMap<>();
    private FXGLGameController gameController;
    private Gson gson = new Gson();
    
    public GameMessageHandler() {
        this.gameController = new FXGLGameController();
    }
    
    public void registerPlayerChannel(String playerId, WebRTCDataChannel channel) {
        playerChannels.put(playerId, channel);
        System.out.println("Зарегистрирован канал для игрока " + playerId);
    }
    
    // ОТПРАВИТЬ СООБЩЕНИЕ ВСЕМ КАНАЛАМ (для отладки)
    // ДОБАВИТЬ ЭТОТ МЕТОД В GameMessageHandler
public void broadcastToAll(String jsonMessage) {
    System.out.println("📢 [Handler] РАССЫЛКА ВСЕМ: " + jsonMessage);
    for (Map.Entry<String, WebRTCDataChannel> entry : playerChannels.entrySet()) {
        if (entry.getValue().isOpen()) {
            // ИСПРАВЛЕНО: используем receiveMessage, а не send!
            entry.getValue().receiveMessage(jsonMessage);
            System.out.println("   -> отправлено " + entry.getKey());
        }
    }
}
    
    // ОТПРАВИТЬ СООБЩЕНИЕ КОНКРЕТНОМУ КАНАЛУ
    public void sendToPlayer(String playerId, String jsonMessage) {
    WebRTCDataChannel channel = playerChannels.get(playerId);
    if (channel != null && channel.isOpen()) {
        // ИСПРАВЛЕНО: используем receiveMessage, а не send!
        channel.receiveMessage(jsonMessage);
        System.out.println("📨 [Handler] Отправлено игроку " + playerId);
    } else {
        System.err.println("❌ [Handler] Не удалось отправить игроку " + playerId);
    }
}
    public GameSession getPlayerSession(String playerId) {
    String sessionId = playerToSession.get(playerId);
    if (sessionId == null) {
        System.out.println("⚠️ [Handler] Игрок " + playerId + " не найден в сессии");
        return null;
    }
    
    GameSession session = activeSessions.get(sessionId);
    if (session == null) {
        System.out.println("⚠️ [Handler] Сессия " + sessionId + " не найдена");
        return null;
    }
    
    return session;
}
    // ============ ИГРОК ОТПРАВЛЯЕТ СООБЩЕНИЕ ============
    public void handlePlayerMessage(String playerId, String jsonMessage) {
        System.out.println("\n🎮 [Handler] Сообщение от игрока " + playerId);
        System.out.println("   " + jsonMessage);
        
        Map<String, Object> msgMap = gson.fromJson(jsonMessage, Map.class);
        String type = (String) msgMap.get("type");
        
        if ("handshake".equals(type)) {
            // 1. Создаем сессию
            String gameType = (String) msgMap.get("game");
            String playerName = (String) msgMap.get("playerName");
            
            GameSession session = new GameSession(gameType, playerId, playerName);
            activeSessions.put(session.getSessionId(), session);
            playerToSession.put(playerId, session.getSessionId());
            System.out.println("✅ Создана сессия: " + session.getSessionId());
            
            // 2. Отправляем подтверждение игроку
            Map<String, Object> ack = new HashMap<>();
            ack.put("type", "handshake_ack");
            ack.put("game", gameType);
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", "accepted");
            payload.put("reason", "waiting_for_opponent");
            ack.put("payload", payload);
            sendToPlayer(playerId, gson.toJson(ack));
            
            // 3. РАССЫЛАЕМ handshake ВСЕМ (чтобы бот получил!)
            broadcastToAll(jsonMessage);
            
        } else if ("move".equals(type)) {
            // Пересылаем ход оппоненту
            String sessionId = playerToSession.get(playerId);
            if (sessionId != null) {
                GameSession session = activeSessions.get(sessionId);
                if (session != null && session.getOpponentId() != null) {
                    sendToPlayer(session.getOpponentId(), jsonMessage);
                }
            }
        }
    }
    
    // ============ БОТ ОТПРАВЛЯЕТ СООБЩЕНИЕ ============
    public void handleBotMessage(String botId, String jsonMessage) {
        System.out.println("\n🤖 [Handler] Сообщение от бота " + botId);
        System.out.println("   " + jsonMessage);
        
        Map<String, Object> msgMap = gson.fromJson(jsonMessage, Map.class);
        String type = (String) msgMap.get("type");
        
        if ("handshake".equals(type)) {
            // БОТ ПРИСОЕДИНЯЕТСЯ К СЕССИИ
            String gameType = (String) msgMap.get("game");
            String botName = (String) msgMap.get("playerName");
            
            // Ищем открытую сессию
            for (GameSession session : activeSessions.values()) {
                if (session.getGameType().equals(gameType) && !session.isFull()) {
                    session.setOpponent(botId, botName);
                    session.setCurrentTurn(session.getPlayerId());
                    playerToSession.put(botId, session.getSessionId());
                    
                    System.out.println("✅ Бот " + botName + " присоединился к сессии " + session.getSessionId());
                    
                    // Оповещаем игрока
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "control");
                    notification.put("controlType", "opponent_joined");
                    notification.put("game", gameType);
                    Map<String, Object> notifPayload = new HashMap<>();
                    notifPayload.put("opponent_id", botId);
                    notifPayload.put("opponent_name", botName);
                    notification.put("payload", notifPayload);
                    sendToPlayer(session.getPlayerId(), gson.toJson(notification));
                    
                    break;
                }
            }
        } else if ("move".equals(type)) {
            // Пересылаем ход оппоненту
            String sessionId = playerToSession.get(botId);
            if (sessionId != null) {
                GameSession session = activeSessions.get(sessionId);
                if (session != null) {
                    sendToPlayer(session.getPlayerId(), jsonMessage);
                }
            }
        }
    }
    
    public void botJoinSession(String botId, String botName, String gameType) {
        // Этот метод теперь не нужен, логика в handleBotMessage
    }
}