package mate.academy.service.notification;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class TelegramNotificationService implements NotificationService {
    private final RestClient restClient;
    private final String apiUrl;
    private final String botToken;
    private final String chatId;
    private final boolean enabled;

    public TelegramNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${telegram.api-url}") String apiUrl,
            @Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.chat-id}") String chatId,
            @Value("${telegram.enabled}") boolean enabled
    ) {
        this.restClient = restClientBuilder.build();
        this.apiUrl = apiUrl;
        this.botToken = botToken;
        this.chatId = chatId;
        this.enabled = enabled;
    }

    @Override
    public void sendMessage(String message) {
        log.info(
                "Telegram sendMessage called. enabled={}, chatIdPresent={}, tokenPresent={}",
                enabled,
                chatId != null && !chatId.isBlank(),
                botToken != null && !botToken.isBlank()
        );
        if (!enabled) {
            log.debug("Telegram notifications are disabled");
            return;
        }
        if (message == null || message.isBlank()) {
            log.warn("Skipping Telegram notification because message is blank");
            return;
        }
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Skipping Telegram notification because bot token or chat id is missing");
            return;
        }

        try {
            log.info("Sending Telegram notification to chatId={}", chatId);
            restClient.post()
                    .uri(buildSendMessageUrl())
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram notification sent successfully");
        } catch (RestClientException e) {
            log.error("Can't send Telegram notification", e);
        }
    }

    private String buildSendMessageUrl() {
        return apiUrl + "/bot" + botToken + "/sendMessage";
    }
}
