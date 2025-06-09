package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.config.TelegramConfig;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
public class TelegramNotificationServiceImpl implements NotificationService {

    private final TelegramConfig telegramConfig;
    private DefaultAbsSender telegramSender;

    @Autowired
    public TelegramNotificationServiceImpl(TelegramConfig telegramConfig) { // Changed from TradingConfig
        this.telegramConfig = telegramConfig;

        String botToken = telegramConfig.getBotToken();

        if (botToken == null || botToken.isEmpty() || "YOUR_TELEGRAM_BOT_TOKEN".equals(botToken)) {
            log.warn("Telegram Bot Token is not configured or is a placeholder. Telegram notifications will be disabled."); // logger to log
            this.telegramSender = null;
        } else {
            DefaultBotOptions botOptions = new DefaultBotOptions();
            // You can customize botOptions here if needed (e.g., proxy settings)
            this.telegramSender = new DefaultAbsSender(botOptions) {
                @Override
                public String getBotToken() {
                    return botToken;
                }
            };
            log.info("TelegramNotificationService initialized with Bot Token."); // logger to log
        }
    }

    @Override
    public boolean sendMessage(String messageText) {
        if (telegramSender == null) {
            log.warn("Telegram sender not initialized (token missing or placeholder). Cannot send message: {}", messageText); // logger to log
            return false;
        }

        String chatId = telegramConfig.getChatId();
        if (chatId == null || chatId.isEmpty() || "YOUR_TELEGRAM_CHAT_ID".equals(chatId)) {
            log.error("Telegram Chat ID is not configured or is a placeholder. Cannot send message."); // logger to log
            return false;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        // You can enable Markdown or HTML parsing if needed:
        // message.setParseMode(ParseMode.MARKDOWN);

        try {
            log.debug("Attempting to send Telegram message to chatId {}: {}", chatId, messageText); // logger to log
            telegramSender.execute(message);
            log.info("Telegram message sent successfully to chatId {}.", chatId); // logger to log
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chatId {}: {}", chatId, e.getMessage(), e); // logger to log
            return false;
        }
    }
}
