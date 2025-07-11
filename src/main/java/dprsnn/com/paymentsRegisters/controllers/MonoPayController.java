package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import dprsnn.com.paymentsRegisters.service.CrmOrderService;
import dprsnn.com.paymentsRegisters.service.ExcelGenerator;
import dprsnn.com.paymentsRegisters.service.TelegramBotNotifier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class MonoPayController {

    private static final Logger logger = LoggerFactory.getLogger(MonoPayController.class);
    private final CrmOrderService crmOrderService;
    private final TelegramBotNotifier telegramBotNotifier;

    public MonoPayController(CrmOrderService crmOrderService, TelegramBotNotifier telegramBotNotifier) {
        this.crmOrderService = crmOrderService;
        this.telegramBotNotifier = telegramBotNotifier;
    }

    @GetMapping("/mono-pay")
    public String showUploadForm() {
        return "mono_pay";
    }

    @PostMapping(value = "/mono-pay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> handleMonoPayUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Будь ласка, виберіть файл для завантаження."
            ));
        }

        try {
            List<Map<String, String>> payments = processMonoPayExcel(file);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Знайдено " + payments.size() + " платежів MonoPay",
                    "payments", payments
            ));
        } catch (IOException e) {
            logger.error("Помилка при обробці MonoPay файлу: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Помилка при обробці файлу: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/mono-pay/make-payments")
    @ResponseBody
    public ResponseEntity<?> processMonoPayments(
            @RequestBody List<PaymentRecord> payments,
            @RequestParam(name = "paymentType", required = true) String paymentType,
            @RequestParam(name = "exportExcel", required = false, defaultValue = "false") boolean exportExcel) {

        try {
            List<Map<String, Object>> results = crmOrderService.makePayments(payments, paymentType);

            if (exportExcel) {
                String fileName = ExcelGenerator.generateAndSavePaymentsExcel(payments, paymentType);
                String fileUrl = "/mono-pay/download/" + fileName;

                File excelFile = new File("exports/" + fileName);
                telegramBotNotifier.sendFileToTelegram(excelFile);


                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Оплати успішно проведено та файл створено",
                        "results", results,
                        "fileUrl", fileUrl
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Оплати успішно проведено",
                        "results", results
                ));
            }
        } catch (Exception e) {
            logger.error("Помилка при проведенні оплат MonoPay: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Помилка при обробці: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/mono-pay/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get("exports/" + fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new IOException("Файл не знайдено: " + fileName);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private List<Map<String, String>> processMonoPayExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> payments = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowNum = findHeaderRow(sheet);
            if (headerRowNum == -1) throw new IOException("Заголовки не знайдено");

            Row headerRow = sheet.getRow(headerRowNum);
            int dateCol = -1, amountCol = -1, feeCol = -1, orderCol = -1;
            List<String> foundHeaders = new ArrayList<>();

            for (Cell cell : headerRow) {
                String value = normalizeHeader(getCellValueAsString(cell));
                foundHeaders.add(value);

                if ("Дата операції".equalsIgnoreCase(value)) dateCol = cell.getColumnIndex();
                else if ("Сума платежу".equalsIgnoreCase(value)) amountCol = cell.getColumnIndex();
                else if ("Комісія банку".equalsIgnoreCase(value)) feeCol = cell.getColumnIndex();
                else if ("Номер заказу".equalsIgnoreCase(value)) orderCol = cell.getColumnIndex();
            }

            logger.info("🔍 Знайдені заголовки: {}", foundHeaders);
            logger.info("📌 Індекси колонок - Дата: {}, Сума: {}, Комісія: {}, Замовлення: {}",
                    dateCol, amountCol, feeCol, orderCol);

            if (dateCol == -1 || amountCol == -1 || feeCol == -1 || orderCol == -1) {
                throw new IOException("Не всі обов’язкові колонки знайдено у файлі.");
            }

            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderId = getCellValueAsString(row.getCell(orderCol));
                if (orderId == null || orderId.isEmpty()) continue;

                Map<String, String> payment = new HashMap<>();
                payment.put("date", getCellValueAsString(row.getCell(dateCol)));
                payment.put("amount", getCellValueAsString(row.getCell(amountCol)));
                payment.put("fee", getCellValueAsString(row.getCell(feeCol)));
                payment.put("order", orderId);

                payments.add(payment);
            }
        }

        return payments;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            List<String> headers = new ArrayList<>();
            for (Cell cell : row) {
                String value = normalizeHeader(getCellValueAsString(cell));
                headers.add(value);
            }

            if (headers.contains("Дата операції") &&
                    headers.contains("Сума платежу") &&
                    headers.contains("Комісія банку") &&
                    headers.contains("Номер заказу")) {

                logger.info("✅ Заголовки знайдено в рядку {}: {}", i, headers);
                return i;
            }

            logger.debug("⛔ Рядок {} не містить усіх потрібних заголовків: {}", i, headers);
        }

        logger.warn("⚠️ Заголовки не знайдено у жодному рядку.");
        return -1;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return header.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Перевірка чи число ціле
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (int) numericValue) {
                    // Якщо значення є цілим числом, повертаємо ціле число як рядок
                    return String.valueOf((int) numericValue);
                } else {
                    // Якщо значення з плаваючою комою, просто повертаємо його як рядок
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }

}
