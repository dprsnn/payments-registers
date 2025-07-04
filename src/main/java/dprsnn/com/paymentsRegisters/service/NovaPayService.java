package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.dto.CrmPayment;
import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import dprsnn.com.paymentsRegisters.repos.CrmCredentialsRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NovaPayService {

    private final SettingsService settingsService;
    private final WebClient webClient;

    public NovaPayService(SettingsService settingsService, CrmCredentialsRepo crmCredentialsRepo, WebClient webClient) {
        this.settingsService = settingsService;
        this.webClient = webClient;
    }

    public List<Map<String, Object>> makePayments(List<PaymentRecord> payments) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (PaymentRecord record : payments) {
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", record.getOrderId());
            result.put("status", "success");

            List<Map<String, String>> steps = new ArrayList<>();
            result.put("steps", steps);

            try {
                Long orderId = Long.valueOf(record.getOrderId());

                // Перевірка чи існує замовлення
                if (!orderExists(orderId)) {
                    steps.add(Map.of(
                            "text", "Замовлення не знайдено",
                            "color", "danger"
                    ));
                    result.put("status", "error");
                    results.add(result);
                    continue;
                }

                // Скасування існуючих оплат
                List<CrmPayment> crmPayments = getPayments(orderId);
                for (CrmPayment payment : crmPayments) {
                    boolean wasCancelled = cancelPayments(orderId, payment);
                    if (wasCancelled) {
                        steps.add(Map.of(
                                "text", "Скасовано оплату: " + payment.getId(),
                                "color", "secondary"
                        ));
                    }
                }

                // Створення нової оплати
                if (createPayment(orderId, record.getAmount(), record.getFormatedDate(), "Післяплата NovaPay", settingsService.getNovaPayId())) {
                    steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
                } else {
                    steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
                }

                // Створення витрати
                if (createExpence(orderId, record.getExpense(), record.getFormatedDate())) {
                    steps.add(Map.of("text", "Додано витрати", "color", "success"));
                } else {
                    steps.add(Map.of("text", "Помилка при створенні витрати", "color", "danger"));
                }

                // Оновлення статусу та кастомного поля
                if (updateStatusAndCustomField(orderId, settingsService.getStatusId(), settingsService.getCustomField(), record.getPaymentDate())) {
                    steps.add(Map.of("text", "Оновлено статус та поле", "color", "success"));
                } else {
                    steps.add(Map.of("text", "Помилка при оновленні статусу", "color", "danger"));
                }

            } catch (Exception e) {
                result.put("status", "error");
                result.put("error", e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private boolean orderExists(Long orderId) {
        String url = "https://openapi.keycrm.app/v1/order/" + orderId;
        int maxRetries = 3; // Максимальна кількість спроб
        int retryDelay = 5; // Затримка між спробами в секундах (при помилці)
        int fixedDelayMs = 2000; // Фіксована затримка 2 секунди перед кожним запитом

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Додаємо фіксовану затримку перед кожним запитом
                sleepForRetry(fixedDelayMs / 1000); // Переводимо мілісекунди в секунди

                Map<String, Object> response = webClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + settingsService.getApiKey())
                        .header("Content-Type", "application/json")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                return response != null && response.containsKey("id");

            } catch (Exception e) {
                if (e.getMessage().contains("429 Too Many Requests") && attempt < maxRetries) {
                    System.err.println("Rate limit exceeded for order " + orderId +
                            ", retrying in " + retryDelay + " seconds (attempt " +
                            attempt + "/" + maxRetries + ")");
                    sleepForRetry(retryDelay);
                    continue;
                }

                System.err.println("Не вдалося перевірити існування замовлення " + orderId);
                System.err.println(e.getMessage());
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean updateStatusAndCustomField(Long orderId, String statusId, String customFieldUuid, String date) {
        String url = "https://openapi.keycrm.app/v1/order/" + orderId;

//        System.out.println(statusId);

        try {
            Map<String, Object> customField = Map.of(
                    "uuid", customFieldUuid,
                    "value", date
            );

            Map<String, Object> payload = Map.of(
                    "status_id", Integer.parseInt(statusId),
                    "custom_fields", List.of(customField)
            );

            webClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + settingsService.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return true;
        } catch (Exception e) {
            System.err.println("Помилка при оновленні статусу/поля для замовлення " + orderId);
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean createExpence(Long orderId, String expense, String date) {
        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/expense";
        try {
            double amount = Double.parseDouble(expense);
            if (amount <= 0) {
                System.out.println("Пропущено створення витрати — сума 0 або менше");
                return true; // Це не помилка
            }

            Map<String, Object> payload = Map.of(
                    "expense_type_id", settingsService.getNovaExpenseId(),
                    "expense_type", "Комисия Nova Pay",
                    "amount", amount,
                    "payment_date", date
            );

            webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + settingsService.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return true;
        } catch (Exception e) {
            System.err.println("Помилка при створенні витрати для замовлення " + orderId);
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean createPayment(Long orderId, String amount, String paymentDate, String paymentMethod, int paymentMethodId) {
        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/payment";
        try {
            Map<String, Object> payload = Map.of(
                    "payment_method_id", paymentMethodId,
                    "payment_method", paymentMethod,
                    "amount", Double.parseDouble(amount),
                    "status", "paid",
                    "payment_date", paymentDate
            );

            webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + settingsService.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return true;
        } catch (Exception e) {
            System.err.println("Помилка при створенні оплати для замовлення " + orderId);
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean cancelPayments(Long orderId, CrmPayment payment) {
        if ("canceled".equalsIgnoreCase(payment.getStatus())) {
            return false; // нічого не робили
        }

        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/payment/" + payment.getId();

        try {
            Map<String, String> payload = Map.of("status", "canceled");

            webClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + settingsService.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return true; // успішно скасовано

        } catch (Exception e) {
            System.err.println("❌ Помилка при скасуванні оплати " + payment.getId() + " для замовлення " + orderId);
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<CrmPayment> getPayments(Long orderId) {
        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "?include=payments";

        try {
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + settingsService.getApiKey())
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> paymentsList = (List<Map<String, Object>>) response.get("payments");

            if (paymentsList == null) {
                return Collections.emptyList();
            }

            return paymentsList.stream()
                    .map(p -> {
                        CrmPayment cp = new CrmPayment();
                        if (p.get("id") instanceof Number) {
                            cp.setId(((Number) p.get("id")).longValue());
                        }
                        if (p.get("status") instanceof String) {
                            cp.setStatus((String) p.get("status"));
                        }
                        return cp;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void sleepForRetry(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
