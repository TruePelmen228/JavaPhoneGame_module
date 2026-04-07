package com.mycompany.javaphonegame_module.game_common;

import java.util.HashMap;
import java.util.Map;

public class TicTacToeModule implements GameModule {
    
    @Override
    public Object initializeGame(GameSession session) {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
        
        Map<String, Object> state = new HashMap<>();
        state.put("board", board);
        state.put("moves", 0);
        
        session.addGameData("board", board);
        session.addGameData("moves", 0);
        
        return state;
    }
    
    @Override
    public boolean validateMove(GameSession session, String playerId, Map<String, Object> moveData) {
        String[][] board = (String[][]) session.getGameData("board");
        int row = ((Double) moveData.get("row")).intValue();
        int col = ((Double) moveData.get("col")).intValue();
        
        if (row < 0 || row >= 3 || col < 0 || col >= 3) return false;
        return board[row][col].isEmpty();
    }
    
    @Override
    public boolean makeMove(GameSession session, String playerId, Map<String, Object> moveData) {
        if (!validateMove(session, playerId, moveData)) return false;
        
        String[][] board = (String[][]) session.getGameData("board");
        int row = ((Double) moveData.get("row")).intValue();
        int col = ((Double) moveData.get("col")).intValue();
        
        String symbol = playerId.equals(session.getPlayerId()) ? "X" : "O";
        board[row][col] = symbol;
        
        int moves = (int) session.getGameData("moves");
        session.addGameData("moves", moves + 1);
        
        return true;
    }
    
    @Override
    public boolean isGameFinished(GameSession session) {
        String[][] board = (String[][]) session.getGameData("board");
        int moves = (int) session.getGameData("moves");
        
        if (moves >= 9) return true;
        return getWinnerId(session) != null;
    }
    
    @Override
    public String getWinnerId(GameSession session) {
        String[][] board = (String[][]) session.getGameData("board");
        
        // Проверка победных комбинаций
        String winner = checkWinner(board);
        if (winner == null) return null;
        
        return winner.equals("X") ? session.getPlayerId() : session.getOpponentId();
    }
    
    private String checkWinner(String[][] board) {
        // Строки и столбцы
        for (int i = 0; i < 3; i++) {
            if (!board[i][0].isEmpty() && 
                board[i][0].equals(board[i][1]) && 
                board[i][1].equals(board[i][2])) {
                return board[i][0];
            }
            if (!board[0][i].isEmpty() && 
                board[0][i].equals(board[1][i]) && 
                board[1][i].equals(board[2][i])) {
                return board[0][i];
            }
        }
        
        // Диагонали
        if (!board[0][0].isEmpty() && 
            board[0][0].equals(board[1][1]) && 
            board[1][1].equals(board[2][2])) {
            return board[0][0];
        }
        
        if (!board[0][2].isEmpty() && 
            board[0][2].equals(board[1][1]) && 
            board[1][1].equals(board[2][0])) {
            return board[0][2];
        }
        
        return null;
    }
    
    @Override
    public Object getGameState(GameSession session, String playerId) {
        Map<String, Object> state = new HashMap<>();
        state.put("board", session.getGameData("board"));
        state.put("currentTurn", session.getCurrentTurn());
        state.put("yourTurn", session.isMyTurn(playerId));
        state.put("yourSymbol", playerId.equals(session.getPlayerId()) ? "X" : "O");
        state.put("gameActive", !isGameFinished(session));
        return state;
    }
    
    @Override
    public String getGameName() { return "tic-tac-toe"; }
    
    @Override
    public int getMinPlayers() { return 2; }
    
    @Override
    public int getMaxPlayers() { return 2; }
    
    @Override
    public void cleanup(GameSession session) {
        session.getGameData().remove("board");
        session.getGameData().remove("moves");
        session.getGameData().remove("gameModule");
    }
    
    @Override
    public void pause(GameSession session) {}
    
    @Override
    public void resume(GameSession session) {}
}