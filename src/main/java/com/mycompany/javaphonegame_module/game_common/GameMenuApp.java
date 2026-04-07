package com.mycompany.javaphonegame_module.game_common;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class GameMenuApp extends Application {
    
    private GameMessageHandler handler;
    private FXGLGameController gameController;
    private WebRTCDataChannel playerChannel;
    private GameBot bot;
    private Stage gameStage;
    private Stage mainStage;
    private GridPane boardGrid;
    private Label statusLabel;
    private String currentGameType;
    private String playerId = "player1";
    private Gson gson = new Gson();
    private boolean isMyTurn = true;
    private boolean gameActive = true;
    private Button[][] boardButtons = new Button[3][3];
    
    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        System.out.println("✅ MainStage сохранен");
        
        // Создаем контроллер и обработчик
        gameController = new FXGLGameController();
        handler = new GameMessageHandler();
        
        // Создаем канал игрока
        playerChannel = new WebRTCDataChannel("player_channel");
        handler.registerPlayerChannel(playerId, playerChannel);
        
        // Игрок слушает свой канал
        playerChannel.setMessageHandler(message -> {
            System.out.println("\n📨 [UI] ИГРОК получил сообщение: " + message);
            handlePlayerMessage(message);
        });
        
        // Создаем бота
        bot = new GameBot("bot1", "SuperBot", handler);
        
        // UI Меню
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20;");
        
        Text title = new Text("Добро пожаловать в игровое приложение!");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        Text subtitle = new Text("Выберите игру:");
        subtitle.setStyle("-fx-font-size: 16px;");
        
        Button ticTacToeBtn = new Button("Крестики-нолики (против бота)");
        ticTacToeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        ticTacToeBtn.setPrefWidth(250);
        ticTacToeBtn.setOnAction(e -> startGame("tic-tac-toe"));
        
        root.getChildren().addAll(title, subtitle, ticTacToeBtn);
        
        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("Game Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ UI меню отображено");
    }
    
    private void startGame(String gameType) {
        currentGameType = gameType;
        isMyTurn = true;
        gameActive = true;
        
        System.out.println("\n🎮 Запуск игры: " + gameType);
        
        // Очищаем доску
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (boardButtons[i][j] != null) {
                    boardButtons[i][j].setText(" ");
                    boardButtons[i][j].setDisable(false);
                    boardButtons[i][j].setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");
                }
            }
        }
        
        // Создаем окно игры
        gameStage = new Stage();
        gameStage.setTitle(gameType + " - Игра против бота");
        gameStage.setOnCloseRequest(e -> {
            System.out.println("❌ Закрытие окна игры");
            sendPlayerLeft();
            if (mainStage != null) {
                mainStage.show();
            }
        });
        
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20;");
        
        statusLabel = new Label("Подключение к игре...");
        statusLabel.setFont(Font.font(16));
        statusLabel.setTextFill(Color.BLUE);
        
        // Создаем игровое поле
        createTicTacToeBoard(root);
        
        Button backButton = new Button("Выйти в меню");
        backButton.setStyle("-fx-font-size: 14px; -fx-padding: 8;");
        backButton.setOnAction(e -> {
            System.out.println("🔙 Выход в меню");
            sendPlayerLeft();
            gameStage.close();
            if (mainStage != null) {
                mainStage.show();
            }
        });
        
        root.getChildren().add(backButton);
        
        Scene scene = new Scene(root, 600, 700);
        gameStage.setScene(scene);
        gameStage.show();
        
        // Скрываем главное меню
        if (mainStage != null) {
            mainStage.hide();
        }
        
        // Отправляем handshake
        sendHandshake(gameType);
        
        // Запускаем бота
        bot.startGame(gameType);
    }
    
    private void createTicTacToeBoard(VBox root) {
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(5);
        boardGrid.setVgap(5);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button button = new Button(" ");
                button.setPrefSize(80, 80);
                button.setFont(Font.font(24));
                button.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");
                
                final int row = i;
                final int col = j;
                button.setOnAction(e -> makeMove(row, col));
                
                boardButtons[row][col] = button;
                boardGrid.add(button, j, i);
            }
        }
        
        root.getChildren().addAll(statusLabel, boardGrid);
    }
    
    private void sendHandshake(String gameType) {
        Map<String, Object> handshake = new HashMap<>();
        handshake.put("type", "handshake");
        handshake.put("game", gameType);
        handshake.put("playerName", "Player");
        
        String json = gson.toJson(handshake);
        System.out.println("📤 Отправка handshake: " + json);
        
        playerChannel.send(json);
        handler.handlePlayerMessage(playerId, json);
    }
    
    private void sendPlayerLeft() {
        if (!gameActive) return;
        
        Map<String, Object> leftMsg = new HashMap<>();
        leftMsg.put("type", "control");
        leftMsg.put("controlType", "player_left");
        leftMsg.put("game", currentGameType);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("player_id", playerId);
        payload.put("reason", "left_menu");
        leftMsg.put("payload", payload);
        
        String json = gson.toJson(leftMsg);
        playerChannel.send(json);
        handler.handlePlayerMessage(playerId, json);
        
        gameActive = false;
        System.out.println("📤 Отправлено сообщение об уходе игрока");
    }
    
    private void makeMove(int row, int col) {
        if (!gameActive) {
            statusLabel.setText("Игра уже завершена!");
            statusLabel.setTextFill(Color.RED);
            return;
        }
        
        if (!isMyTurn) {
            statusLabel.setText("Сейчас ход бота, подождите!");
            statusLabel.setTextFill(Color.RED);
            return;
        }
        
        if (!boardButtons[row][col].getText().equals(" ")) {
            statusLabel.setText("Эта клетка уже занята!");
            statusLabel.setTextFill(Color.RED);
            return;
        }
        
        System.out.println("\n👆 Игрок делает ход: [" + row + "," + col + "]");
        
        // Получаем сессию
        GameSession session = handler.getPlayerSession(playerId);
        if (session == null) {
            System.err.println("❌ Сессия не найдена!");
            return;
        }
        
        // Делегируем ход контроллеру
        boolean success = gameController.makeMove(session, row, col);
        
        if (success) {
            // Обновляем UI из состояния контроллера
            updateBoardFromController(session);
            
            // Проверяем завершение игры
            if (gameController.isGameFinished(session)) {
                GameResult result = gameController.getGameResult(session);
                handleGameOver(result);
                return;
            }
            
            isMyTurn = false;
            statusLabel.setText("Ход отправлен, бот думает...");
            statusLabel.setTextFill(Color.ORANGE);
            
            // Отправляем ход боту
            Map<String, Object> move = new HashMap<>();
            move.put("type", "move");
            move.put("game", currentGameType);
            move.put("moveId", String.valueOf(System.currentTimeMillis()));
            
            Map<String, Object> coords = new HashMap<>();
            coords.put("row", row);
            coords.put("col", col);
            move.put("coordinates", coords);
            
            String json = gson.toJson(move);
            playerChannel.send(json);
            handler.handlePlayerMessage(playerId, json);
        } else {
            statusLabel.setText("Некорректный ход!");
            statusLabel.setTextFill(Color.RED);
        }
    }
    
    private void updateBoardFromController(GameSession session) {
        String[][] board = gameController.getBoard(session);
        if (board != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (!board[i][j].equals("") && !board[i][j].equals(boardButtons[i][j].getText())) {
                        boardButtons[i][j].setText(board[i][j]);
                        boardButtons[i][j].setDisable(true);
                        
                        if (board[i][j].equals("X")) {
                            boardButtons[i][j].setStyle("-fx-background-color: #a8e6cf; -fx-border-color: #cccccc; -fx-font-size: 24px;");
                        } else if (board[i][j].equals("O")) {
                            boardButtons[i][j].setStyle("-fx-background-color: #ffd3b6; -fx-border-color: #cccccc; -fx-font-size: 24px;");
                        }
                    }
                }
            }
        }
    }
    
    private void handleGameOver(GameResult result) {
        gameActive = false;
        isMyTurn = false;
        
        if (result.winnerId != null && !result.isDraw) {
        if (result.winnerId.equals(playerId)) {
            statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
            statusLabel.setTextFill(Color.GREEN);
            System.out.println("🏆 Игрок победил!");
        } else {
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
            statusLabel.setTextFill(Color.RED);
            System.out.println("💀 Игрок проиграл");
        }
    } 
        
        // Блокируем все кнопки
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setDisable(true);
            }
        }
    }
    
    private void handlePlayerMessage(String message) {
        try {
            Map<String, Object> msgMap = gson.fromJson(message, Map.class);
            String type = (String) msgMap.get("type");
            
            System.out.println("📨 [UI] Тип сообщения: " + type);
            
            if ("move".equals(type) && gameActive) {
                // Ход от бота
                Map<String, Object> coords = (Map<String, Object>) msgMap.get("coordinates");
                int row = ((Double) coords.get("row")).intValue();
                int col = ((Double) coords.get("col")).intValue();
                
                System.out.println("🤖 Ход бота: [" + row + "," + col + "]");
                
                // Получаем сессию
                GameSession session = handler.getPlayerSession(playerId);
                if (session == null) return;
                
                // Обновляем доску через контроллер
                boolean success = gameController.makeMove(session, row, col);
                
                if (success) {
                    updateBoardFromController(session);
                    
                    // Проверяем завершение игры
                    if (gameController.isGameFinished(session)) {
                        GameResult result = gameController.getGameResult(session);
                        handleGameOver(result);
                    } else {
                        isMyTurn = true;
                        statusLabel.setText("✅ Ваш ход!");
                        statusLabel.setTextFill(Color.GREEN);
                    }
                }
            }
            else if ("control".equals(type)) {
                String controlType = (String) msgMap.get("controlType");
                
                if ("opponent_joined".equals(controlType)) {
                    System.out.println("✅ Бот подключился!");
                    statusLabel.setText("✅ Бот подключился! Ваш ход!");
                    statusLabel.setTextFill(Color.GREEN);
                    isMyTurn = true;
                    gameActive = true;
                } 
                else if ("player_left".equals(controlType)) {
                    System.out.println("⚠️ Соперник покинул игру");
                    statusLabel.setText("⚠️ Соперник покинул игру");
                    statusLabel.setTextFill(Color.RED);
                    gameActive = false;
                    isMyTurn = false;
                    
                    // Разблокируем кнопку выхода
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            boardButtons[i][j].setDisable(true);
                        }
                    }
                }
            }
            else if ("game_over".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) msgMap.get("payload");
                String result = (String) payload.get("result");
                
                if ("win".equals(result)) {
                    statusLabel.setText("🎉 ПОБЕДА! 🎉");
                } else if ("lose".equals(result)) {
                    statusLabel.setText("😢 ПОРАЖЕНИЕ! 😢");
                } else if ("draw".equals(result)) {
                    statusLabel.setText("🤝 НИЧЬЯ! 🤝");
                }
                statusLabel.setTextFill(Color.GREEN);
                gameActive = false;
                isMyTurn = false;
            }
            else if ("handshake_ack".equals(type)) {
                System.out.println("✅ Handshake подтвержден");
                statusLabel.setText("⏳ Ожидание подключения бота...");
                statusLabel.setTextFill(Color.BLUE);
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}