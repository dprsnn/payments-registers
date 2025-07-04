//package dprsnn.com.paymentsRegisters.service;
//
//import dprsnn.com.paymentsRegisters.controllers.UkrPostController;
//import dprsnn.com.paymentsRegisters.dto.CrmPayment;
//import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
//import dprsnn.com.paymentsRegisters.models.UkrPost;
//import dprsnn.com.paymentsRegisters.repos.UkrPostRepo;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class MonoPayService {
//    private final SettingsService settingsService;
//    private final WebClient webClient;
//    private final UkrPostRepo ukrPostRepo;
//    private static final Logger logger = LoggerFactory.getLogger(MonoPayService.class);
//
//
//    public MonoPayService(SettingsService settingsService, WebClient webClient, UkrPostRepo ukrPostRepo) {
//        this.settingsService = settingsService;
//        this.webClient = webClient;
//        this.ukrPostRepo = ukrPostRepo;
//    }
//
//    public List<Map<String, Object>> makePayments(List<PaymentRecord> payments, String paymentType) {
//        // пошук айдішок
//        if (paymentType.equals("mono_pay_uht"))
//            setOrderId(payments, settingsService.getUhtSourceId());
//        else if (paymentType.equals("mono_pay_hh")) {
//            setOrderId(payments, settingsService.getHHSourceId());
//        }
//        else if (paymentType.equals("mono_pay_swell")) {
//            setOrderId(payments, settingsService.getSwellSourceId());
//        }
//        else if (paymentType.equals("evo_pay")) {
//            setOrderId(payments, -1);
//        }
//        else if (paymentType.equals("ukrpost")) {
//            setUkrpostOrderId(payments);
//        }
//
//        List<Map<String, Object>> results = new ArrayList<>();
//
//        for (PaymentRecord paymentRecord : payments) {
//            Map<String, Object> result = new HashMap<>();
//            result.put("orderId", paymentRecord.getSourceOrderId());  // Додаємо OrderId для кожного платежу
//            result.put("status", "success");
//
//            List<Map<String, String>> steps = new ArrayList<>();
//            result.put("steps", steps);
//
//            try {
//                if (paymentRecord.getOrderId().equals("-1")) {
//                    steps.add(Map.of(
//                            "text", "Замовлення не знайдено",
//                            "color", "danger"
//                    ));
//                    result.put("status", "error");
//                } else {
//                    // Замовлення знайдено, додаємо його OrderId до результату
//                    steps.add(Map.of(
//                            "text", "Замовлення знайдено, CRM ID: " + paymentRecord.getOrderId(),
//                            "color", "success"
//                    ));
//
//                    // Відміна всіх оплат
//                    List<CrmPayment> crmPayments = getPayments(Long.valueOf(paymentRecord.getOrderId()));
//                    for (CrmPayment payment : crmPayments) {
//                        boolean wasCancelled = cancelPayments(Long.valueOf(paymentRecord.getOrderId()), payment);
//                        if (wasCancelled) {
//                            steps.add(Map.of(
//                                    "text", "Скасовано оплату: " + payment.getId(),
//                                    "color", "secondary"
//                            ));
//                        }
//                    }
//
//                    // Додавання оплат
//                    if (paymentType.equals("mono_pay_uht")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDatePlusOneDay(), "Еквайрінг Моно uht.net.ua", settingsService.getMonoUhtId())){
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    } else if (paymentType.equals("mono_pay_hh")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDatePlusOneDay(), "Еквайрінг Моно hh.in.ua", settingsService.getMonoHHId())){
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    } else if (paymentType.equals("mono_pay_swell")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDatePlusOneDay(), "Плата бай Моно Септівел", settingsService.getMonoSwellId())){
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    } else if (paymentType.equals("evo_pay")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDate(), "Evopay", settingsService.getEvoPayId())){
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    } else if (paymentType.equals("ukrpost")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDate(), "Укр Пошта", settingsService.getUkrpostId())){
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    } else if (paymentType.equals("nova_pay")){
//                        if (createPayment(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getAmount(), paymentRecord.getFormatedDate(), "Післяплата NovaPay", settingsService.getNovaPayId())) {
//                            steps.add(Map.of("text", "Створено нову оплату", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при створенні оплати", "color", "danger"));
//                        }
//                    }
//
//
//                    // Створення витрати
//                    if (createExpence(Long.valueOf(paymentRecord.getOrderId()), paymentRecord.getExpense(), paymentRecord.getFormatedDatePlusOneDay(), paymentType)) {
//                        steps.add(Map.of("text", "Додано витрати", "color", "success"));
//                    } else {
//                        steps.add(Map.of("text", "Помилка при створенні витрати", "color", "danger"));
//                    }
//
//                    // Оновлення статусу та кастомного поля
//                    if (paymentType.equals("evo_pay") || paymentType.equals("ukrpost") || paymentType.equals("nova_pay")){
//                        if (updateStatusAndCustomField(Long.valueOf(paymentRecord.getOrderId()), settingsService.getStatusId(), settingsService.getCustomField(), paymentRecord.getFormatedDate())) {
//                            steps.add(Map.of("text", "Оновлено статус та поле", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при оновленні статусу", "color", "danger"));
//                        }
//                    }   else {
//                        if (updateStatusAndCustomField(Long.valueOf(paymentRecord.getOrderId()), settingsService.getStatusId(), settingsService.getCustomField(), paymentRecord.getFormatedDatePlusOneDay())) {
//                            steps.add(Map.of("text", "Оновлено статус та поле", "color", "success"));
//                        } else {
//                            steps.add(Map.of("text", "Помилка при оновленні статусу", "color", "danger"));
//                        }
//                    }
//
//                }
//            } catch (Exception e) {
//                result.put("status", "error");
//                result.put("error", e.getMessage());
//                steps.add(Map.of(
//                        "text", "Помилка: " + e.getMessage(),
//                        "color", "danger"
//                ));
//            }
//
//            results.add(result);
//        }
//
//        return results;
//    }
//
//    private void setUkrpostOrderId(List<PaymentRecord> payments) {
//        for (PaymentRecord paymentRecord : payments) {
//            try {
//                Optional<UkrPost> ukrPostOptional = ukrPostRepo.findById(paymentRecord.getTtn());
//                if (ukrPostOptional.isPresent()) {
//                    paymentRecord.setOrderId(String.valueOf(ukrPostOptional.get().getCrmId()));
//                } else {
//                    logger.warn("Не знайдено запису UkrPost для TTN: {}", paymentRecord.getTtn());
//                    paymentRecord.setOrderId("-1"); // або інше значення за замовчуванням
//                }
//            } catch (Exception e) {
//                logger.error("Помилка при отриманні orderId для TTN: {}", paymentRecord.getTtn(), e);
//                paymentRecord.setOrderId("ERROR"); // або інше значення для помилок
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private void setOrderId(List<PaymentRecord> payments, int monoId) {
//        for (PaymentRecord payment : payments) {
//            String sourceOrderId = payment.getSourceOrderId();
////            System.out.println("📝 Запит до API з sourceOrderId: " + sourceOrderId);
//            String url;
//            if (monoId == -1){
//                 url = String.format(
//                        "https://openapi.keycrm.app/v1/order?filter[source_uuid]=%s",
//                        sourceOrderId
//                );
//            } else {
//                 url = String.format(
//                        "https://openapi.keycrm.app/v1/order?filter[source_id]=%d&filter[source_uuid]=%s",
//                        monoId, sourceOrderId
//                );
//            }
//
//            System.out.println("📍 URL запиту: " + url);
//
//            try {
//                Map<String, Object> response = webClient.get()
//                        .uri(url)
//                        .header("Authorization", "Bearer " + settingsService.getApiKey())
//                        .retrieve()
//                        .bodyToMono(Map.class)
//                        .block();
//
//                if (response == null) {
////                    System.out.println("⚠️ Відповідь від API порожня для sourceOrderId: " + sourceOrderId);
//                    // Якщо відповідь порожня, встановлюємо orderId = -1
//                    payment.setOrderId("-1");
//                    continue;
//                }
//
////                System.out.println("📥 Отримано відповідь: " + response);
//
//                if (response.containsKey("data")) {
//                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
//
//                    // Шукаємо замовлення з відповідним source_uuid
//                    Optional<Map<String, Object>> targetOrder = data.stream()
//                            .filter(order -> sourceOrderId.equals(String.valueOf(order.get("source_uuid"))))
//                            .findFirst();
//
//                    if (targetOrder.isPresent()) {
//                        Object orderIdObj = targetOrder.get().get("id");
//                        String orderId = (orderIdObj instanceof Number)
//                                ? String.valueOf(((Number) orderIdObj).intValue())
//                                : String.valueOf(orderIdObj);
//
//                        payment.setOrderId(orderId);
//                        System.out.println("✅ Для sourceOrderId=" + sourceOrderId + " знайдено orderId=" + orderId);
//                    } else {
//                        // Якщо замовлення не знайдено, встановлюємо orderId = -1
//                        payment.setOrderId("-1");
//                        System.out.println("ℹ️ Замовлення з source_uuid=" + sourceOrderId + " не знайдено в API. Встановлено orderId = -1");
//                    }
//                } else {
////                    System.out.println("⚠️ Відповідь API не містить поля 'data' для sourceOrderId: " + sourceOrderId);
//                    // Якщо немає поля 'data', встановлюємо orderId = -1
//                    payment.setOrderId("-1");
//                }
//            } catch (Exception e) {
//                System.err.println("❌ Помилка при запиті для sourceOrderId: " + sourceOrderId);
//                e.printStackTrace();
//                // Встановлюємо orderId = -1 в разі помилки
//                payment.setOrderId("-1");
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public List<CrmPayment> getPayments(Long orderId) {
//        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "?include=payments";
//
//        try {
//            Map<String, Object> response = webClient.get()
//                    .uri(url)
//                    .header("Authorization", "Bearer " + settingsService.getApiKey())
//                    .header("Content-Type", "application/json")
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            if (response == null) {
//                return Collections.emptyList();
//            }
//
//            List<Map<String, Object>> paymentsList = (List<Map<String, Object>>) response.get("payments");
//
//            if (paymentsList == null) {
//                return Collections.emptyList();
//            }
//
//            return paymentsList.stream()
//                    .map(p -> {
//                        CrmPayment cp = new CrmPayment();
//                        if (p.get("id") instanceof Number) {
//                            cp.setId(((Number) p.get("id")).longValue());
//                        }
//                        if (p.get("status") instanceof String) {
//                            cp.setStatus((String) p.get("status"));
//                        }
//                        return cp;
//                    })
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Collections.emptyList();
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private boolean cancelPayments(Long orderId, CrmPayment payment) {
//        if ("canceled".equalsIgnoreCase(payment.getStatus())) {
//            return false; // нічого не робили
//        }
//
//        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/payment/" + payment.getId();
//
//        try {
//            Map<String, String> payload = Map.of("status", "canceled");
//
//            webClient.put()
//                    .uri(url)
//                    .header("Authorization", "Bearer " + settingsService.getApiKey())
//                    .header("Content-Type", "application/json")
//                    .bodyValue(payload)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            return true; // успішно скасовано
//
//        } catch (Exception e) {
//            System.err.println("❌ Помилка при скасуванні оплати " + payment.getId() + " для замовлення " + orderId);
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private boolean createPayment(Long orderId, String amount, String paymentDate, String paymentMethod, int paymentMethodId) {
//        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/payment";
//        try {
//            // Замінюємо кому на крапку і видаляємо пробіли
//            String normalizedAmount = amount.replace(",", ".").replace(" ", "");
//            double amountValue = Double.parseDouble(normalizedAmount);
//
//            Map<String, Object> payload = Map.of(
//                    "payment_method_id", paymentMethodId,
//                    "payment_method", paymentMethod,
//                    "amount", amountValue,  // Використовуємо вже перетворене значення
//                    "status", "paid",
//                    "payment_date", paymentDate
//            );
//
//            webClient.post()
//                    .uri(url)
//                    .header("Authorization", "Bearer " + settingsService.getApiKey())
//                    .header("Content-Type", "application/json")
//                    .bodyValue(payload)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            return true;
//        } catch (Exception e) {
//            System.err.println("Помилка при створенні оплати для замовлення " + orderId);
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private boolean createExpence(Long orderId, String expense, String date, String paymentType) {
//        String url = "https://openapi.keycrm.app/v1/order/" + orderId + "/expense";
//        try {
//            // Перевіряємо на null і порожній рядок
//            if (expense == null || expense.trim().isEmpty()) {
//                logger.info("Пропущено створення витрати — сума не вказана");
//                return true;
//            }
//
//            double amount = Double.parseDouble(expense);
//            if (amount <= 0) {
//                System.out.println("Пропущено створення витрати — сума 0 або менше");
//                return true; // Це не помилка
//            }
//            Map<String, Object> payload;
//
//            if (paymentType.equals("evo_pay")){
//                payload = Map.of(
//                        "expense_type_id", settingsService.getEvoExpenseId(),
//                        "expense_type", "Комисия Evo Pay",
//                        "amount", amount,
//                        "payment_date", date
//                );
//            } else if(paymentType.equals("nova_pay")) {
//                payload = Map.of(
//                        "expense_type_id", settingsService.getNovaExpenseId(),
//                        "expense_type", "Комисия Nova Pay",
//                        "amount", amount,
//                        "payment_date", date
//                );
//            } else {
//                payload = Map.of(
//                        "expense_type_id", settingsService.getMonoExpenseId(),
//                        "expense_type", "Комисия Mono Pay",
//                        "amount", amount,
//                        "payment_date", date
//                );
//            }
//
//
//
//            webClient.post()
//                    .uri(url)
//                    .header("Authorization", "Bearer " + settingsService.getApiKey())
//                    .header("Content-Type", "application/json")
//                    .bodyValue(payload)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            return true;
//        } catch (Exception e) {
//            System.err.println("Помилка при створенні витрати для замовлення " + orderId);
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private boolean updateStatusAndCustomField(Long orderId, String statusId, String customFieldUuid, String date) {
//        String url = "https://openapi.keycrm.app/v1/order/" + orderId;
//
////        System.out.println(statusId);
//
//        try {
//            Map<String, Object> customField = Map.of(
//                    "uuid", customFieldUuid,
//                    "value", date
//            );
//
//            Map<String, Object> payload = Map.of(
//                    "status_id", Integer.parseInt(statusId),
//                    "custom_fields", List.of(customField)
//            );
//
//            webClient.put()
//                    .uri(url)
//                    .header("Authorization", "Bearer " + settingsService.getApiKey())
//                    .header("Content-Type", "application/json")
//                    .bodyValue(payload)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            return true;
//        } catch (Exception e) {
//            System.err.println("Помилка при оновленні статусу/поля для замовлення " + orderId);
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//}