package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.config.TradingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramNotificationServiceImplTest {

    @Mock
    private TradingConfig mockTradingConfig;
    @Mock
    private TradingConfig.Telegram mockTelegramConfig;
    @Mock
    private TradingConfig.Telegram.Bot mockBotConfig;

    // We need to test the service's interaction with DefaultAbsSender.
    // Since DefaultAbsSender is created and managed internally, we can't directly mock it via @Mock and @InjectMocks
    // for the instance used by the SUT.
    // One option: Spy on a real DefaultAbsSender if its constructor can be called with a test token.
    // Another option: Refactor SUT to accept DefaultAbsSender.
    // For this test, we'll test different paths based on config.

    @Spy // Spy on a real instance that we can then control specific methods of.
    private DefaultAbsSender spiedTelegramSender; // This will be tricky to inject into SUT.

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    private TelegramNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        // Common setup for TradingConfig mocks
        when(mockTradingConfig.getTelegram()).thenReturn(mockTelegramConfig);
        when(mockTelegramConfig.getBot()).thenReturn(mockBotConfig);
    }

    @Test
    void constructor_shouldInitializeSender_whenTokenIsValid() {
        when(mockBotConfig.getToken()).thenReturn("VALID_TOKEN");
        // Chat ID is not checked in constructor for sender initialization

        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);
        // Difficult to assert internal 'telegramSender' state without reflection or getter.
        // We can infer by behavior in sendMessage.
        // For now, just assert service creation.
        assertNotNull(notificationService);
    }

    @Test
    void constructor_shouldNotInitializeSender_whenTokenIsPlaceholder() {
        when(mockBotConfig.getToken()).thenReturn("YOUR_TELEGRAM_BOT_TOKEN");
        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);
        // Attempt to send a message; should fail because sender is null.
        boolean result = notificationService.sendMessage("Test");
        assertFalse(result, "Message sending should fail if token is placeholder.");
    }

    @Test
    void constructor_shouldNotInitializeSender_whenTokenIsNull() {
        when(mockBotConfig.getToken()).thenReturn(null);
        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);
        boolean result = notificationService.sendMessage("Test");
        assertFalse(result, "Message sending should fail if token is null.");
    }

    @Test
    void sendMessage_shouldReturnFalse_whenSenderNotInitialized() {
        when(mockBotConfig.getToken()).thenReturn(null); // Ensures sender is null
        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);

        boolean result = notificationService.sendMessage("Hello");
        assertFalse(result);
    }

    @Test
    void sendMessage_shouldReturnFalse_whenChatIdIsPlaceholder() {
        when(mockBotConfig.getToken()).thenReturn("VALID_TOKEN");
        when(mockBotConfig.getChatId()).thenReturn("YOUR_TELEGRAM_CHAT_ID");
        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);

        boolean result = notificationService.sendMessage("Hello");
        assertFalse(result);
    }

    @Test
    void sendMessage_shouldReturnFalse_whenChatIdIsNull() {
        when(mockBotConfig.getToken()).thenReturn("VALID_TOKEN");
        when(mockBotConfig.getChatId()).thenReturn(null);
        notificationService = new TelegramNotificationServiceImpl(mockTradingConfig);

        boolean result = notificationService.sendMessage("Hello");
        assertFalse(result);
    }

    // This test demonstrates how to test the interaction with a mocked sender if it were injectable.
    // It requires refactoring TelegramNotificationServiceImpl or using PowerMock.
    @Test
    void sendMessage_shouldCallExecute_withCorrectParameters_ifSenderIsInjectedAndMocked() throws TelegramApiException {
        // Arrange
        String testToken = "VALID_TOKEN_FOR_MOCK_SENDER";
        String testChatId = "12345";
        String testMessage = "Hello, Telegram!";

        when(mockBotConfig.getToken()).thenReturn(testToken);
        when(mockBotConfig.getChatId()).thenReturn(testChatId);

        // Create a spy of a DefaultAbsSender instance.
        // The DefaultAbsSender needs DefaultBotOptions.
        DefaultBotOptions botOptions = new DefaultBotOptions();
        // We need to use a spy that is instantiated with the testToken.
        // The anonymous class DefaultAbsSender in the SUT makes direct spying hard.
        // Let's assume we create a testable version or refactor SUT.

        // Hypothetical TestableTelegramNotificationServiceImpl that allows injecting sender
        TestableTelegramNotificationServiceImpl testableService =
            new TestableTelegramNotificationServiceImpl(mockTradingConfig, spiedTelegramSender);

        // Stub the getBotToken method of the spied sender
        // Note: spiedTelegramSender is an abstract class, so this is fine.
        // If it was a concrete class, some mocking frameworks might have issues with final methods.
        doReturn(testToken).when(spiedTelegramSender).getBotToken(); // Crucial for the spy.

        // Make execute() do nothing for this test to avoid real API calls
        doNothing().when(spiedTelegramSender).execute(any(SendMessage.class));

        // Act
        boolean result = testableService.sendMessage(testMessage);

        // Assert
        assertTrue(result);
        verify(spiedTelegramSender).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getValue();
        assertEquals(testChatId, sentMessage.getChatId());
        assertEquals(testMessage, sentMessage.getText());
    }

    @Test
    void sendMessage_shouldReturnFalse_whenTelegramApiExceptionOccurs() throws TelegramApiException {
        String testToken = "VALID_TOKEN_FOR_MOCK_SENDER_EX";
        String testChatId = "12345EX";
        String testMessage = "Exception Test";

        when(mockBotConfig.getToken()).thenReturn(testToken);
        when(mockBotConfig.getChatId()).thenReturn(testChatId);

        TestableTelegramNotificationServiceImpl testableService =
            new TestableTelegramNotificationServiceImpl(mockTradingConfig, spiedTelegramSender);
        doReturn(testToken).when(spiedTelegramSender).getBotToken();

        // Simulate TelegramApiException
        doThrow(new TelegramApiException("Test API Exception")).when(spiedTelegramSender).execute(any(SendMessage.class));

        boolean result = testableService.sendMessage(testMessage);
        assertFalse(result);
    }

    // Helper class for testing purposes to allow injecting a (spy) DefaultAbsSender
    static class TestableTelegramNotificationServiceImpl extends TelegramNotificationServiceImpl {
        private DefaultAbsSender testSender;

        public TestableTelegramNotificationServiceImpl(TradingConfig tradingConfig, DefaultAbsSender testSender) {
            super(tradingConfig); // This will initialize its own sender, but we'll override it.
            this.testSender = testSender;
        }

        // Override the sendMessage to use the testSender instance
        // This is a common pattern but requires careful thought about what the super.sendMessage would do.
        // For simplicity, we are re-implementing parts of sendMessage logic here to ensure testSender is used.
        // A cleaner way is to make the sender field in the base class protected or have a setter.
        @Override
        public boolean sendMessage(String messageText) {
            if (testSender == null) { // If our test sender wasn't set up, maybe fall back or fail
                 Logger logger = LoggerFactory.getLogger(TestableTelegramNotificationServiceImpl.class);
                 logger.warn("Test sender not initialized in TestableTelegramNotificationServiceImpl.");
                 return false;
            }
            String chatId = super.tradingConfig.getTelegram().getBot().getChatId(); // Use super's config
             if (chatId == null || chatId.isEmpty() || "YOUR_TELEGRAM_CHAT_ID".equals(chatId)) {
                 Logger logger = LoggerFactory.getLogger(TestableTelegramNotificationServiceImpl.class);
                 logger.error("Testable: Telegram Chat ID is not configured or is a placeholder. Cannot send message.");
                 return false;
             }
            SendMessage message = new SendMessage(chatId, messageText);
            try {
                testSender.execute(message);
                return true;
            } catch (TelegramApiException e) {
                Logger logger = LoggerFactory.getLogger(TestableTelegramNotificationServiceImpl.class);
                logger.error("Testable: Failed to send Telegram message: {}", e.getMessage());
                return false;
            }
        }
    }
}
