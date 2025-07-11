package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramBotNotifier {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    public void sendFileToTelegram(File file) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendDocument";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("document", new FileSystemResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("✅ Файл надіслано в Telegram");
        } else {
            System.out.println("❌ Помилка надсилання: " + response.getBody());
        }
    }

    public void logErrorForTelegram(PaymentRecord paymentRecord, String paymentType, String errorType) {
        String message = buildErrorMessage(paymentRecord, paymentType, errorType);
        sendTextMessageToTelegram(message);
    }

    private String buildErrorMessage(PaymentRecord paymentRecord, String paymentType, String errorType) {
        String errorDescription = "";

        // Визначаємо опис помилки в залежності від errorType
        if ("id".equals(errorType)) {
            errorDescription = "Не знайдено замовлення";
        } else if ("payment".equals(errorType)) {
            errorDescription = "Помилка при проведенні оплати";
        }

        // Формуємо повідомлення в залежності від типу платежу
        switch (paymentType) {
            case "mono_pay_uht":
            case "mono_pay_hh":
            case "mono_pay_swell":
            case "evo_pay":
                return String.format(" %s №%s (Тип: %s)",
                        errorDescription, paymentRecord.getSourceOrderId(), paymentType);
            case "ukrpost":
                return String.format(" %s ТТН %s (Тип: %s)",
                        errorDescription, paymentRecord.getTtn(), paymentType);
            case "nova_pay":
                return String.format(" %s ID %s (Тип: %s)",
                        errorDescription, paymentRecord.getOrderId(), paymentType);
            default:
                return String.format(" Невідома помилка (Тип: %s)", paymentType);
        }
    }

    public void sendTextMessageToTelegram(String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", message);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Повідомлення надіслано в Telegram: " + message);
            } else {
                System.out.println("❌ Помилка надсилання повідомлення: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("❌ Помилка при відправці в Telegram: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
