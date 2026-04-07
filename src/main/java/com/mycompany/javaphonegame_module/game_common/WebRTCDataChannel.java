package com.mycompany.javaphonegame_module.game_common;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * WebRTCDataChannel - только заглушка канала связи
 * Отвечает только за отправку/прием сообщений и уведомление слушателей
 */
public class WebRTCDataChannel {
    private Consumer<String> messageHandler;
    private List<String> messageHistory = new ArrayList<>();
    private boolean isOpen = true;
    private String channelId;
    
    static {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    
    public WebRTCDataChannel(String channelId) {
        this.channelId = channelId;
    }
    
    public void send(String message) {
        if (!isOpen) {
            System.out.println("Канал " + channelId + " закрыт, нельзя отправить сообщение");
            return;
        }
        
        System.out.println("\n📤 [WebRTC-" + channelId + "] Отправка сообщения:");
        System.out.println("   " + message);
        messageHistory.add(message);
    }
    
    // Этот метод вызывается внешним обработчиком при получении сообщения
    public void receiveMessage(String message) {
        if (!isOpen) {
            System.out.println("Канал закрыт, нельзя принять сообщение");
            return;
        }
        
        System.out.println("\n📥 [WebRTC-" + channelId + "] Получено сообщение:");
        System.out.println("   " + message);
        messageHistory.add(message);
        
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }
    
    public void close() {
        isOpen = false;
        System.out.println("🔌 [WebRTC-" + channelId + "] Канал закрыт");
    }
    
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void clearHistory() {
        messageHistory.clear();
    }
}