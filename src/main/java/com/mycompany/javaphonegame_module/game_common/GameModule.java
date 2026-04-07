package com.mycompany.javaphonegame_module.game_common;

import java.util.Map;

public interface GameModule {
    // Основные методы
    Object initializeGame(GameSession session);
    boolean validateMove(GameSession session, String playerId, Map<String, Object> moveData);
    boolean makeMove(GameSession session, String playerId, Map<String, Object> moveData);
    boolean isGameFinished(GameSession session);
    String getWinnerId(GameSession session); // null если нет победителя или ничья
    Object getGameState(GameSession session, String playerId);
    String getGameName();
    int getMinPlayers();
    int getMaxPlayers();
    void cleanup(GameSession session);
    void pause(GameSession session);
    void resume(GameSession session);
}

// Простой класс для результата игры (можно сделать record если Java 14+)
class GameResult {
    public final boolean isFinished;
    public final String winnerId;
    public final String winnerName;
    public final boolean isDraw;
    public final String message;
    
    private GameResult(boolean isFinished, String winnerId, String winnerName, boolean isDraw, String message) {
        this.isFinished = isFinished;
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.isDraw = isDraw;
        this.message = message;
    }
    
    public static GameResult notFinished() {
        return new GameResult(false, null, null, false, null);
    }
    
    public static GameResult win(String winnerId, String winnerName) {
        return new GameResult(true, winnerId, winnerName, false, null);
    }
    
    public static GameResult draw(String message) {
        return new GameResult(true, null, null, true, message);
    }
}