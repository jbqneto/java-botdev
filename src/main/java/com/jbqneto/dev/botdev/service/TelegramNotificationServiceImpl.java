package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.config.TelegramConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramNotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationServiceImpl.class);

    private final TelegramConfig telegramConfig; // Changed from TradingConfig
    private DefaultAbsSender telegramSender;

    @Autowired
    public TelegramNotificationServiceImpl(TelegramConfig telegramConfig) { // Changed from TradingConfig
        this.telegramConfig = telegramConfig;

        String botToken = telegramConfig.getBotToken();

        if (botToken == null || botToken.isEmpty() || "YOUR_TELEGRAM_BOT_TOKEN".equals(botToken)) {
            logger.warn("Telegram Bot Token is not configured or is a placeholder. Telegram notifications will be disabled.");
            this.telegramSender = null;
        } else {
            DefaultBotOptions botOptions = new DefaultBotOptions();
            // You can customize botOptions here if needed (e.g., proxy settings)
            this.telegramSender = new DefaultAbsSender(botOptions) {
                @Override
                public String getBotToken() {
                    return botToken; // This correctly uses the botToken from the outer class's scope
                }
            };
            logger.info("TelegramNotificationService initialized with Bot Token.");
        }
    }

    @Override
    public boolean sendMessage(String messageText) {
        if (telegramSender == null) {
            logger.warn("Telegram sender not initialized (token missing or placeholder). Cannot send message: {}", messageText);
            return false;
        }

        String chatId = telegramConfig.getChatId();
        if (chatId == null || chatId.isEmpty() || "YOUR_TELEGRAM_CHAT_ID".equals(chatId)) {
            logger.error("Telegram Chat ID is not configured or is a placeholder. Cannot send message.");
            return false;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        // You can enable Markdown or HTML parsing if needed:
        // message.setParseMode(ParseMode.MARKDOWN);

        try {
            logger.debug("Attempting to send Telegram message to chatId {}: {}", chatId, messageText);
            telegramSender.execute(message);
            logger.info("Telegram message sent successfully to chatId {}.", chatId);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Failed to send Telegram message to chatId {}: {}", chatId, e.getMessage(), e);
            return false;
        }
    }
}
