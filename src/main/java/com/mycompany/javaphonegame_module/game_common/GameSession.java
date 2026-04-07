/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphonegame_module.game_common;

/**
 *
 * @author Марина
 */

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameSession {
    private final String sessionId;
    private final String gameType;
    private final String playerId;
    private final String playerName;
    private final LocalDateTime startTime;
    private boolean isPaused;
    private LocalDateTime pauseTime;
    private Map<String, Object> gameData;
    private String opponentId;      // для игр на двоих
    private String opponentName;    // имя оппонента
    private String currentTurn;     // чей сейчас ход
    
    public GameSession(String gameType, String playerId, String playerName) {
        this.sessionId = UUID.randomUUID().toString();
        this.gameType = gameType;
        this.playerId = playerId;
        this.playerName = playerName;
        this.startTime = LocalDateTime.now();
        this.isPaused = false;
        this.gameData = new HashMap<>();
        this.opponentId = null;
        this.opponentName = null;
        this.currentTurn = playerId; // первый игрок ходит первым
    }
    
    // Конструктор для игры на двоих
    public GameSession(String gameType, String player1Id, String player1Name, 
                      String player2Id, String player2Name) {
        this.sessionId = UUID.randomUUID().toString();
        this.gameType = gameType;
        this.playerId = player1Id;
        this.playerName = player1Name;
        this.opponentId = player2Id;
        this.opponentName = player2Name;
        this.startTime = LocalDateTime.now();
        this.isPaused = false;
        this.gameData = new HashMap<>();
        this.currentTurn = player1Id; // первый игрок ходит первым
    }
    
    // Геттеры
    public String getSessionId() {
        return sessionId;
    }
    
    public String getGameType() {
        return gameType;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public void setPaused(boolean paused) {
        isPaused = paused;
        if (paused) {
            this.pauseTime = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getPauseTime() {
        return pauseTime;
    }
    
    public Map<String, Object> getGameData() {
        return gameData;
    }
    
    public void setGameData(Map<String, Object> gameData) {
        this.gameData = gameData;
    }
    
    public void addGameData(String key, Object value) {
        this.gameData.put(key, value);
    }
    
    public Object getGameData(String key) {
        return this.gameData.get(key);
    }
    
    public String getOpponentId() {
        return opponentId;
    }
    
    public void setOpponentId(String opponentId) {
        this.opponentId = opponentId;
    }
    
    public String getOpponentName() {
        return opponentName;
    }
    
    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }
    
    // Удобный метод для установки обоих параметров оппонента сразу
    public void setOpponent(String opponentId, String opponentName) {
        this.opponentId = opponentId;
        this.opponentName = opponentName;
    }
    
    public String getCurrentTurn() {
        return currentTurn;
    }
    
    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }
    
    public boolean isMyTurn(String playerId) {
        return currentTurn != null && currentTurn.equals(playerId);
    }
    
    public void switchTurn() {
        if (currentTurn != null && opponentId != null) {
            currentTurn = currentTurn.equals(playerId) ? opponentId : playerId;
        }
    }
    
    // Получение имени текущего игрока, который должен ходить
    public String getCurrentTurnName() {
        if (currentTurn == null) return null;
        return currentTurn.equals(playerId) ? playerName : opponentName;
    }
    
    // Получение информации о противнике
    public String getOpponentInfo() {
        if (opponentName != null) {
            return opponentName + " (" + opponentId + ")";
        } else if (opponentId != null) {
            return opponentId;
        }
        return "Ожидание игрока";
    }
    
    // Проверка, все ли игроки на месте
    public boolean isFull() {
        return opponentId != null && opponentName != null;
    }
    
    // Проверка, есть ли оппонент (даже если имени нет)
    public boolean hasOpponent() {
        return opponentId != null;
    }
    
    // Длительность игры в секундах
    public long getDurationSeconds() {
        if (isPaused && pauseTime != null) {
            return Duration.between(startTime, pauseTime).getSeconds();
        }
        return Duration.between(startTime, LocalDateTime.now()).getSeconds();
    }
    
    // Получение всех ID игроков в сессии
    public String[] getAllPlayerIds() {
        if (opponentId != null) {
            return new String[]{playerId, opponentId};
        }
        return new String[]{playerId};
    }
    
    // Получение всех имен игроков в сессии
    public String[] getAllPlayerNames() {
        if (opponentName != null) {
            return new String[]{playerName, opponentName};
        }
        return new String[]{playerName};
    }
    
    // Проверка, является ли игрок участником этой сессии
    public boolean isParticipant(String playerId) {
        return this.playerId.equals(playerId) || 
               (opponentId != null && opponentId.equals(playerId));
    }
    
    @Override
    public String toString() {
        return "GameSession{" +
                "sessionId='" + sessionId + '\'' +
                ", gameType='" + gameType + '\'' +
                ", playerId='" + playerId + '\'' +
                ", playerName='" + playerName + '\'' +
                ", opponentId='" + opponentId + '\'' +
                ", opponentName='" + opponentName + '\'' +
                ", isPaused=" + isPaused +
                ", currentTurn='" + currentTurn + '\'' +
                '}';
    }
}