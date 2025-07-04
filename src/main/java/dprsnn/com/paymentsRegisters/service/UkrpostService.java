package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.models.Log;
import dprsnn.com.paymentsRegisters.models.UkrPost;
import dprsnn.com.paymentsRegisters.repos.LogRepo;
import dprsnn.com.paymentsRegisters.repos.UkrPostRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UkrpostService {

    private final UkrPostRepo ukrPostRepo;
    private final SettingsService settingsService;
    private final LogRepo logRepo;
    private final WebClient webClient;

    public UkrpostService(UkrPostRepo ukrPostRepo, SettingsService settingsService, LogRepo logRepo, WebClient webClient) {
        this.ukrPostRepo = ukrPostRepo;
        this.settingsService = settingsService;
        this.logRepo = logRepo;
        this.webClient = webClient;
    }
    @Scheduled(cron = "0 0 1 * * ?") // Запускати щодня о 01:00 ночі
    public void scheduledUpdateOrderList() {
        System.out.println("Запуск автоматичної синхронізації УкрПошти: " + LocalDateTime.now());
        try {
            updateOrderList(LocalDate.now(), LocalDate.now().minusDays(1));
            logRepo.save(new Log("Автоматична синхронізація УкрПошти успішна", LocalDateTime.now()));
        } catch (Exception e) {
            logRepo.save(new Log("Помилка автоматичної синхронізації УкрПошти: " + e.getMessage(), LocalDateTime.now()));
            System.err.println("Помилка при автоматичній синхронізації: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void updateOrderList(LocalDate today, LocalDate startDate) {
        System.out.println("Початок синхронізації даних УкрПошти: " + LocalDateTime.now());


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int page = 1;
        int limit = 100;
        int addedCount = 0;
        int skippedCount = 0;
        int totalProcessed = 0;

        try {
            while (true) {
                String url = String.format(
                        "https://openapi.keycrm.app/v1/order?limit=%d&page=%d&sort=id" +
                                "&filter[has_tracking_code]=true" +
                                "&filter[created_between]=%s 00:00:00,%s 23:59:59" +
                                "&include=shipping",
                        limit, page, formatter.format(startDate), formatter.format(today)
                );

//                System.out.println("URL - " + url);

                Map<String, Object> response = webClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + settingsService.getApiKey())
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

//                System.out.println("Відповідь API: " + response);

                if (response == null || !response.containsKey("data")) {
                    break;
                }

                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (data == null || data.isEmpty()) {
                    break;
                }

                for (Map<String, Object> order : data) {
                    totalProcessed++;
                    try {
                        Map<String, Object> shipping = (Map<String, Object>) order.get("shipping");
                        if (shipping != null && shipping.get("tracking_code") != null) {
                            String ttn = String.valueOf(shipping.get("tracking_code")).trim();
                            Long crmId = ((Number) order.get("id")).longValue();

                            if (!ukrPostRepo.existsById(ttn)) {
                                UkrPost record = new UkrPost();
                                record.setTtn(ttn);
                                record.setCrmId(crmId);
                                ukrPostRepo.save(record);
                                addedCount++;
                                System.out.println("✅ Додано: TTN=" + ttn + ", CRM ID=" + crmId);
                            } else {
                                skippedCount++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Помилка при обробці замовлення: " + order);
                        e.printStackTrace();
                    }
                }

                page++;
            }
        } finally {
            System.out.println("Завершення синхронізації. Оброблено: " + totalProcessed +
                    ", Додано: " + addedCount +
                    ", Пропущено: " + skippedCount);

            logRepo.save(new Log("УкрПошта: Синхронізація завершена" + " Оброблено замовлень: " + totalProcessed + " Додано нових записів: " + addedCount + " Пропущено існуючих записів: " + skippedCount + " За період з " + today + " по " + startDate, LocalDateTime.now()));
        }
    }

}
