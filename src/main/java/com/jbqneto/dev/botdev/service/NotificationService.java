package com.jbqneto.dev.botdev.service;

/**
 * Interface for sending notifications.
 */
public interface NotificationService {

    /**
     * Sends a message to a configured notification channel.
     *
     * @param messageText The text of the message to send.
     * @return true if the message was sent successfully (or queued for sending), false otherwise.
     */
    boolean sendMessage(String messageText);
}
