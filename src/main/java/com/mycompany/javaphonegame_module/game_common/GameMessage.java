/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphonegame_module.game_common;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
public class GameMessage {
    private String type;           // handshake, move, move_response, game_over, control, error
    private String game;           // tic-tac-toe, sea_battle, chess
    private String moveId;         // для move
    private String inResponseTo;   // для move_response
    private String controlType;    // для control (pause, resume, player_left)
    private Integer errorCode;     // для error
    private String errorType;      // для error
    private String message;        // для error и game_over
    private Map<String, Object> payload;  // все остальные данные
    
    public GameMessage() {}
    
    public GameMessage(String type, String game) {
        this.type = type;
        this.game = game;
    }
    
    
    public static GameMessage handshake(String game, String playerId, String playerName) {
        GameMessage msg = new GameMessage("handshake", game);
        Map<String, Object> payload = new HashMap<>();
        payload.put("player_id", playerId);
        payload.put("player_name", playerName);
        msg.setPayload(payload);
        return msg;
    }
    
    public static GameMessage handshakeAck(String game, boolean accepted, String reason) {
        GameMessage msg = new GameMessage("handshake_ack", game);
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", accepted ? "accepted" : "rejected");
        if (reason != null) payload.put("reason", reason);
        msg.setPayload(payload);
        return msg;
    }
    
    public static GameMessage move(String game, String moveId, Map<String, Object> moveData) {
        GameMessage msg = new GameMessage("move", game);
        msg.setMoveId(moveId);
        msg.setPayload(moveData);
        return msg;
    }
    
    public static GameMessage moveResponse(String game, String inResponseTo, 
                                          String status, String result, String message, 
                                          Object gameState) {
        GameMessage msg = new GameMessage("move_response", game);
        msg.setInResponseTo(inResponseTo);
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        if (result != null) payload.put("result", result);
        if (message != null) payload.put("message", message);
        if (gameState != null) payload.put("game_state", gameState);
        msg.setPayload(payload);
        return msg;
    }
    
    public static GameMessage gameOver(String game, String result, String winner, 
                                      String winnerName, String reason, String message) {
        GameMessage msg = new GameMessage("game_over", game);
        Map<String, Object> payload = new HashMap<>();
        payload.put("result", result);
        if (winner != null) payload.put("winner", winner);
        if (winnerName != null) payload.put("winner_name", winnerName);
        if (reason != null) payload.put("reason", reason);
        if (message != null) payload.put("message", message);
        msg.setPayload(payload);
        return msg;
    }
    
    public static GameMessage control(String game, String controlType, Map<String, Object> data) {
        GameMessage msg = new GameMessage("control", game);
        msg.setControlType(controlType);
        msg.setPayload(data);
        return msg;
    }
    
    public static GameMessage error(int errorCode, String errorType, String message, Object details) {
        GameMessage msg = new GameMessage("error", null);
        msg.setErrorCode(errorCode);
        msg.setErrorType(errorType);
        msg.setMessage(message);
        if (details != null) {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("received_type", details);
            msg.setPayload(detailsMap);
        }
        return msg;
    }
    
    // from-to json
    public String toJson() {
        return new Gson().toJson(this);
    }
    
    public static GameMessage fromJson(String json) {
        return new Gson().fromJson(json, GameMessage.class);
    }
    
    // additional funcs
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCoordinates() {
        if (payload != null && payload.containsKey("coordinates")) {
            return (Map<String, Object>) payload.get("coordinates");
        }
        return null;
    }
    
    public String getPlayerId() {
        return payload != null ? (String) payload.get("player_id") : null;
    }
    
    public String getPlayerName() {
        return payload != null ? (String) payload.get("player_name") : null;
    }
    
    public String getStatus() {
        return payload != null ? (String) payload.get("status") : null;
    }
    
    public String getReason() {
        return payload != null ? (String) payload.get("reason") : null;
    }
    
    // getters+setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public String getMoveId() { return moveId; }
    public void setMoveId(String moveId) { this.moveId = moveId; }
    public String getInResponseTo() { return inResponseTo; }
    public void setInResponseTo(String inResponseTo) { this.inResponseTo = inResponseTo; }
    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
