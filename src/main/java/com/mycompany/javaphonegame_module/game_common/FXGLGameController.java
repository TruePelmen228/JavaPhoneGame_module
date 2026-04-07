package com.mycompany.javaphonegame_module.game_common;

import java.util.HashMap;
import java.util.Map;

public class FXGLGameController {
    private final Map<String, GameSession> activeGames = new HashMap<>();
    
    public FXGLGameController() {
        System.out.println("🎮 FXGLGameController создан");
    }
    
    // ============ УПРАВЛЕНИЕ СЕССИЕЙ ============
    
    public void startGame(String gameType, GameSession session) {
        System.out.println("🎮 [Controller] Запуск игры " + gameType + " для сессии " + session.getSessionId());
        
        // Инициализируем доску
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
        
        session.addGameData("board", board);
        session.addGameData("gameActive", true);  // Явно устанавливаем true
        session.addGameData("winner", null);
        
        activeGames.put(session.getSessionId(), session);
        
        System.out.println("   Доска инициализирована, gameActive = true");
        System.out.println("   session.gameActive = " + session.getGameData("gameActive"));
    }
    
    // ============ ХОДЫ ============
    
    public boolean makeMove(GameSession session, int row, int col) {
        if (session == null) {
            System.err.println("❌ [Controller] Session is null");
            return false;
        }
        
        // Получаем gameActive из сессии
        Object gameActiveObj = session.getGameData("gameActive");
        System.out.println("🔍 [Controller] gameActive из сессии: " + gameActiveObj);
        
        boolean gameActive = false;
        if (gameActiveObj instanceof Boolean) {
            gameActive = (Boolean) gameActiveObj;
        } else {
            System.err.println("❌ [Controller] gameActive не Boolean или null: " + gameActiveObj);
            // Если нет gameActive, устанавливаем true
            gameActive = true;
            session.addGameData("gameActive", true);
        }
        
        if (!gameActive) {
            System.out.println("❌ [Controller] Игра не активна");
            return false;
        }
        
        // Получаем текущую доску из сессии
        String[][] currentBoard = (String[][]) session.getGameData("board");
        
        // Если доски нет - создаем новую
        if (currentBoard == null) {
            System.err.println("❌ [Controller] Board is null, initializing...");
            currentBoard = new String[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    currentBoard[i][j] = "";
                }
            }
            session.addGameData("board", currentBoard);
        }
        
        // Проверяем границы
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            System.out.println("❌ [Controller] Неверные координаты");
            return false;
        }
        
        // Проверяем, свободна ли клетка
        if (currentBoard[row][col] == null || !currentBoard[row][col].equals("")) {
            System.out.println("❌ [Controller] Клетка [" + row + "," + col + "] уже занята");
            return false;
        }
        
        // Определяем, кто ходит
        String currentPlayer = session.getCurrentTurn();
        if (currentPlayer == null) {
            System.err.println("❌ [Controller] currentPlayer is null");
            return false;
        }
        
        String symbol = currentPlayer.equals(session.getPlayerId()) ? "X" : "O";
        
        // Делаем ход
        currentBoard[row][col] = symbol;
        session.addGameData("board", currentBoard);
        
        System.out.println("✅ [Controller] Ход " + symbol + " на [" + row + "," + col + "]");
        
        // Проверяем победу
        String winner = checkWinner(currentBoard);
        if (winner != null) {
            System.out.println("🏆 [Controller] Победитель: " + winner);
            session.addGameData("gameActive", false);
            session.addGameData("winner", winner);
            return true;
        }
        
        // Проверяем ничью
        if (isBoardFull(currentBoard)) {
            System.out.println("🤝 [Controller] Ничья!");
            session.addGameData("gameActive", false);
            session.addGameData("winner", "draw");
            return true;
        }
        
        // Меняем очередь хода
        session.switchTurn();
        System.out.println("🔄 [Controller] Теперь ход: " + session.getCurrentTurn());
        
        return true;
    }
    
    // ============ ПРОВЕРКА ПОБЕДЫ ============
    
    private String checkWinner(String[][] board) {
        if (board == null) return null;
        
        // Проверка строк
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != null && !board[i][0].equals("") && 
                board[i][0].equals(board[i][1]) && 
                board[i][1].equals(board[i][2])) {
                return board[i][0];
            }
        }
        
        // Проверка столбцов
        for (int j = 0; j < 3; j++) {
            if (board[0][j] != null && !board[0][j].equals("") && 
                board[0][j].equals(board[1][j]) && 
                board[1][j].equals(board[2][j])) {
                return board[0][j];
            }
        }
        
        // Проверка диагоналей
        if (board[0][0] != null && !board[0][0].equals("") && 
            board[0][0].equals(board[1][1]) && 
            board[1][1].equals(board[2][2])) {
            return board[0][0];
        }
        
        if (board[0][2] != null && !board[0][2].equals("") && 
            board[0][2].equals(board[1][1]) && 
            board[1][1].equals(board[2][0])) {
            return board[0][2];
        }
        
        return null;
    }
    
    private boolean isBoardFull(String[][] board) {
        if (board == null) return false;
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == null || board[i][j].equals("")) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // ============ ПОЛУЧЕНИЕ СОСТОЯНИЯ ============
    
    public boolean isGameFinished(GameSession session) {
        if (session == null) return false;
        Boolean active = (Boolean) session.getGameData("gameActive");
        return active != null && !active;
    }
    
    public GameResult getGameResult(GameSession session) {
        if (session == null) return GameResult.notFinished();
        
        String winner = (String) session.getGameData("winner");
        
        if (winner == null) {
            return GameResult.notFinished();
        } else if (winner.equals("draw")) {
            return GameResult.draw("Ничья!");
        } else {
            String winnerId = winner.equals("X") ? session.getPlayerId() : session.getOpponentId();
            String winnerName = winner.equals("X") ? session.getPlayerName() : session.getOpponentName();
            return GameResult.win(winnerId, winnerName);
        }
    }
    
    public Object getGameState(GameSession session, String playerId) {
        Map<String, Object> state = new HashMap<>();
        state.put("board", session.getGameData("board"));
        state.put("currentTurn", session.getCurrentTurn());
        state.put("yourTurn", session.isMyTurn(playerId));
        state.put("gameActive", session.getGameData("gameActive"));
        state.put("yourSymbol", playerId.equals(session.getPlayerId()) ? "X" : "O");
        return state;
    }
    
    public String[][] getBoard(GameSession session) {
        if (session == null) return null;
        return (String[][]) session.getGameData("board");
    }
    
    // ============ УПРАВЛЕНИЕ ИГРОКОМ ============
    
    public void handlePlayerLeft(GameSession session, String playerId) {
        System.out.println("🚪 [Controller] Игрок " + playerId + " покинул игру");
        
        if (session != null) {
            session.addGameData("gameActive", false);
            session.addGameData("winner", "left");
        }
    }
    
    public void cleanupGame(GameSession session) {
        if (session == null) return;
        System.out.println("🧹 [Controller] Очистка игры для сессии " + session.getSessionId());
        activeGames.remove(session.getSessionId());
    }
    
    public void pauseGame(GameSession session) {
        if (session == null) return;
        System.out.println("⏸️ [Controller] Пауза игры");
        session.setPaused(true);
    }
    
    public void resumeGame(GameSession session) {
        if (session == null) return;
        System.out.println("▶️ [Controller] Продолжение игры");
        session.setPaused(false);
    }
    
    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============
    
    public boolean isGameActive(GameSession session) {
        if (session == null) return false;
        Boolean active = (Boolean) session.getGameData("gameActive");
        return active != null && active;
    }
    
    public String getWinner(GameSession session) {
        if (session == null) return null;
        return (String) session.getGameData("winner");
    }
}