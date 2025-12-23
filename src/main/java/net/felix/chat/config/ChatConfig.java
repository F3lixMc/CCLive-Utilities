package net.felix.chat.config;

/**
 * Konfiguration für das Chat-System
 */
public class ChatConfig {
    // Polling-Intervall für neue Nachrichten (in Millisekunden)
    private int messagePollInterval = 2000; // 2 Sekunden
    
    // Timeout für Chat-Requests
    private int connectionTimeout = 5000;
    private int readTimeout = 10000;
    
    public ChatConfig() {
    }
    
    // Getter
    public int getMessagePollInterval() { return messagePollInterval; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
}

