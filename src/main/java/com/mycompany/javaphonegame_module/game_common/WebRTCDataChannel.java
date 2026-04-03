/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphonegame_module.game_common;

/**
 *
 * @author Марина
 */
class WebRTCDataChannel {
    void send(String message);      // Отправка JSON через WebRTC
    void onMessage(String message); // Получение JSON из WebRTC
    void close();                   // Закрытие канала
}

// Их реальная реализация (примерно так):
public class RealWebRTCDataChannel implements WebRTCDataChannel {
    private DataChannel nativeWebRTCChannel; // нативный WebRTC канал
    
    @Override
    public void send(String message) {
        // Отправка JSON через WebRTC
        nativeWebRTCChannel.send(message.getBytes());
    }
    
    @Override
    public void onMessage(String message) {
        // Вызывается, когда приходит сообщение из WebRTC
        // Они просто передают JSON дальше в ваш модуль
    }
    
    @Override
    public void close() {
        nativeWebRTCChannel.close();
    }
}
