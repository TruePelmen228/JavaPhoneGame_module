/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphonegame_module.game_common;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author Марина
 */

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameMessageHandler {
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>(); // sessionId -> session
    private final Map<String, String> playerToSession = new ConcurrentHashMap<>(); // playerId -> sessionId
    private WebRTCDataChannel dataChannel;
    private FXGLGameController gameController;
    
    public GameMessageHandler() {
        this.gameController = new FXGLGameController();
    }
    
    private void handleHandshake(GameMessage msg) {
        String gameType = msg.getGame();
        String playerId = msg.getPlayerId();
        String playerName = msg.getPlayerName();
        
        System.out.println("Handshake от " + playerName + " для игры " + gameType);
        
        // Проверяем, не в игре ли уже игрок
        if (playerToSession.containsKey(playerId)) {
            GameMessage ack = GameMessage.handshakeAck(gameType, false, "player_already_in_game");
            sendToChannel(ack.toJson());
            return;
        }
        
        // Ищем доступную сессию для этого типа игры
        GameSession existingSession = findAvailableSession(gameType);
        
        if (existingSession == null) {
            // Создаем новую сессию (ждем второго игрока)
            GameSession newSession = new GameSession(gameType, playerId, playerName);
            activeSessions.put(newSession.getSessionId(), newSession);
            playerToSession.put(playerId, newSession.getSessionId());
            
            GameMessage ack = GameMessage.handshakeAck(gameType, true, "waiting_for_opponent");
            sendToChannel(ack.toJson());
            
            System.out.println("Создана новая сессия " + newSession.getSessionId() + " в ожидании игрока");
        } else {
            // Присоединяемся к существующей сессии
            existingSession.setOpponentId(playerId);
            existingSession.setCurrentTurn(existingSession.getPlayerId()); // первый игрок ходит первым
            
            playerToSession.put(playerId, existingSession.getSessionId());
            
            GameMessage ack = GameMessage.handshakeAck(gameType, true, "joined");
            sendToChannel(ack.toJson());
            
            System.out.println("Игрок " + playerName + " присоединился к сессии " + existingSession.getSessionId());
            
            // Оповещаем первого игрока, что второй присоединился
            notifyOpponentJoined(existingSession);
            
            // Запускаем игру
            gameController.startGame(gameType, existingSession);
        }
    }
    
    private GameSession findAvailableSession(String gameType) {
        // Ищем сессию того же типа, которая ждет игрока
        for (GameSession session : activeSessions.values()) {
            if (session.getGameType().equals(gameType) && !session.isFull()) {
                return session;
            }
        }
        return null;
    }
    
    private void notifyOpponentJoined(GameSession session) {
        // Оповещаем первого игрока
        GameMessage notification = GameMessage.control(
            session.getGameType(), 
            "opponent_joined", 
            Map.of("opponent_id", session.getOpponentId())
        );
        
        // Отправляем первому игроку
        String firstPlayerId = session.getPlayerId();
        sendToPlayer(firstPlayerId, notification.toJson());
    }
    
    private void handleMove(GameMessage msg) {
        String playerId = msg.getPlayerId();
        String sessionId = playerToSession.get(playerId);
        
        if (sessionId == null) {
            sendError(1004, "Игрок не в игре", playerId);
            return;
        }
        
        GameSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            sendError(1005, "Сессия не найдена", sessionId);
            return;
        }
        
        // Проверяем, чей ход
        if (!session.isMyTurn(playerId)) {
            GameMessage response = GameMessage.moveResponse(
                msg.getGame(), msg.getMoveId(), "rejected", null, 
                "Сейчас не ваш ход", null
            );
            sendToPlayer(playerId, response.toJson());
            return;
        }
        
        // Проверяем, не на паузе ли игра
        if (session.isPaused()) {
            GameMessage response = GameMessage.moveResponse(
                msg.getGame(), msg.getMoveId(), "rejected", null, 
                "Игра на паузе", null
            );
            sendToPlayer(playerId, response.toJson());
            return;
        }
        
        // Обрабатываем ход в зависимости от игры
        String gameType = msg.getGame();
        String moveId = msg.getMoveId();
        Map<String, Object> coordinates = msg.getCoordinates();
        
        switch (gameType) {
            case "tic-tac-toe":
                processTicTacToeMove(session, msg, moveId, coordinates);
                break;
            case "sea_battle":
                processSeaBattleMove(session, msg, moveId, coordinates);
                break;
            case "chess":
                processChessMove(session, msg, moveId, coordinates);
                break;
            default:
                sendError(1002, "Неизвестная игра", gameType);
        }
    }
    
    private void processTicTacToeMove(GameSession session, GameMessage msg, 
                                     String moveId, Map<String, Object> coordinates) {
        int row = ((Double) coordinates.get("row")).intValue();
        int col = ((Double) coordinates.get("col")).intValue();
        
        boolean isValid = gameController.validateMove(session, row, col);
        
        if (isValid) {
            gameController.makeMove(session, row, col);
            Object gameState = gameController.getCurrentState(session);
            
            // Меняем очередь хода
            session.switchTurn();
            
            // Отправляем ответ ходившему игроку
            GameMessage response = GameMessage.moveResponse(
                msg.getGame(), moveId, "accepted", "success", "Ход принят", gameState
            );
            sendToPlayer(msg.getPlayerId(), response.toJson());
            
            // Отправляем обновление второму игроку
            GameMessage update = GameMessage.moveResponse(
                msg.getGame(), moveId, "accepted", "opponent_move", 
                "Противник сделал ход", gameState
            );
            sendToPlayer(session.getOpponentId(), update.toJson());
            
            checkGameOver(session);
        } else {
            GameMessage response = GameMessage.moveResponse(
                msg.getGame(), moveId, "rejected", null, "Некорректный ход", null
            );
            sendToPlayer(msg.getPlayerId(), response.toJson());
        }
    }
    
    private void checkGameOver(GameSession session) {
        if (gameController.isGameFinished(session)) {
            GameResult result = gameController.getGameResult(session);
            
            GameMessage gameOver;
            if (result.isWin()) {
                gameOver = GameMessage.gameOver(
                    session.getGameType(), "win", result.getWinner(), result.getWinnerName(),
                    null, null
                );
            } else {
                gameOver = GameMessage.gameOver(
                    session.getGameType(), "draw", null, null, "stalemate", "Ничья! Игра завершена"
                );
            }
            
            // Отправляем обоим игрокам
            String gameOverJson = gameOver.toJson();
            sendToPlayer(session.getPlayerId(), gameOverJson);
            if (session.getOpponentId() != null) {
                sendToPlayer(session.getOpponentId(), gameOverJson);
            }
            
            // Удаляем сессию
            cleanupSession(session);
        }
    }
    
    private void handleControl(GameMessage msg) {
        String playerId = msg.getPlayerId();
        String sessionId = playerToSession.get(playerId);
        
        if (sessionId == null) {
            sendError(1004, "Игрок не в игре", playerId);
            return;
        }
        
        GameSession session = activeSessions.get(sessionId);
        String controlType = msg.getControlType();
        
        switch (controlType) {
            case "pause":
                if (!session.isPaused()) {
                    session.setPaused(true);
                    gameController.pauseGame(session);
                    
                    // Оповещаем обоих игроков
                    broadcastToSession(session, msg.toJson());
                }
                break;
                
            case "resume":
                if (session.isPaused()) {
                    session.setPaused(false);
                    gameController.resumeGame(session);
                    
                    // Оповещаем обоих игроков
                    broadcastToSession(session, msg.toJson());
                }
                break;
                
            case "player_left":
                String leftPlayerId = (String) msg.getPayload().get("player_id");
                handlePlayerLeft(session, leftPlayerId);
                break;
                
            default:
                sendError(1003, "Неизвестный тип control", controlType);
        }
    }
    
    private void handlePlayerLeft(GameSession session, String playerId) {
        GameMessage leftMsg = GameMessage.control(
            session.getGameType(), "player_left", 
            Map.of("player_id", playerId, "reason", "disconnected")
        );
        
        // Оповещаем другого игрока
        String otherPlayerId = playerId.equals(session.getPlayerId()) ? 
                              session.getOpponentId() : session.getPlayerId();
        
        if (otherPlayerId != null) {
            sendToPlayer(otherPlayerId, leftMsg.toJson());
        }
        
        // Очищаем сессию
        cleanupSession(session);
    }
    
    private void broadcastToSession(GameSession session, String message) {
        sendToPlayer(session.getPlayerId(), message);
        if (session.getOpponentId() != null) {
            sendToPlayer(session.getOpponentId(), message);
        }
    }
    
    private void sendToPlayer(String playerId, String jsonMessage) {
        // Здесь должна быть логика отправки конкретному игроку
        // через WebRTC DataChannel
        if (dataChannel != null) {
            // Можно добавить идентификатор получателя в сообщение
            dataChannel.send(jsonMessage);
        }
    }
    
    private void cleanupSession(GameSession session) {
        playerToSession.remove(session.getPlayerId());
        if (session.getOpponentId() != null) {
            playerToSession.remove(session.getOpponentId());
        }
        activeSessions.remove(session.getSessionId());
        gameController.cleanupGame(session);
    }
    
    private void sendError(int errorCode, String message, Object details) {
        GameMessage error = GameMessage.error(errorCode, "protocol_error", message, details);
        sendToChannel(error.toJson());
    }
    
    private void sendToChannel(String jsonMessage) {
        if (dataChannel != null) {
            dataChannel.send(jsonMessage);
            System.out.println("Sent: " + jsonMessage);
        }
    }
    
    public void setDataChannel(WebRTCDataChannel channel) {
        this.dataChannel = channel;
    }
}
