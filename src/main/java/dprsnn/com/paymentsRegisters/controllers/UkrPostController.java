package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import dprsnn.com.paymentsRegisters.service.CrmOrderService;
import dprsnn.com.paymentsRegisters.service.ExcelGenerator;
import dprsnn.com.paymentsRegisters.service.UkrpostService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UkrPostController {

    private static final Logger logger = LoggerFactory.getLogger(UkrPostController.class);
    private final UkrpostService ukrpostService;
    private final CrmOrderService crmOrderService;

    public UkrPostController(UkrpostService ukrpostService, CrmOrderService crmOrderService) {
        this.ukrpostService = ukrpostService;
        this.crmOrderService = crmOrderService;
    }

    @GetMapping("/ukrpost")
    public String showUploadForm() {
        return "ukrpost";
    }

    @GetMapping("/ukrpost-update-yesterday")
    public String updateTtnYesterday() {
        ukrpostService.updateOrderList(LocalDate.now(), LocalDate.now().minusDays(1));
        return "redirect:/ukrpost";
    }

    @GetMapping("/ukrpost-update-month")
    public String updateTtnMonth() {
        ukrpostService.updateOrderList(LocalDate.now(), LocalDate.now().minusDays(30));
        return "redirect:/ukrpost";
    }

    @PostMapping(value = "/ukrpost", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> handleEvoPayUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Будь ласка, виберіть файл для завантаження."
            ));
        }

        try {
            List<Map<String, String>> payments = processUkrpostExcel(file);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Знайдено " + payments.size() + " платежів EvoPay",
                    "payments", payments
            ));
        } catch (IOException e) {
            logger.error("Помилка при обробці EvoPay файлу: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Помилка при обробці файлу: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/ukrpost/make-payments")
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

    private List<Map<String, String>> processUkrpostExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> payments = new ArrayList<>();
        String registryDateFormatted = null;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Отримання дати з першого рядка
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                Cell firstCell = firstRow.getCell(1); // Колонка B
                if (firstCell != null) {
                    String firstRowText = getCellValueAsString(firstCell);
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                    java.util.regex.Matcher matcher = pattern.matcher(firstRowText);
                    if (matcher.find()) {
                        String registryDate = matcher.group();
                        registryDateFormatted = formatDate(registryDate);
                        System.out.println("Дата реєстру: " + registryDateFormatted);
                    }
                }
            }

            int headerRowNum = findHeaderRow(sheet);
            if (headerRowNum == -1) throw new IOException("Заголовки не знайдено");

            Row headerRow = sheet.getRow(headerRowNum);
            int amountCol = -1, orderCol = -1;
            List<String> foundHeaders = new ArrayList<>();

            for (Cell cell : headerRow) {
                String value = normalizeHeader(getCellValueAsString(cell));
                foundHeaders.add(value);

                if ("сума, грн.".equalsIgnoreCase(value)) amountCol = cell.getColumnIndex();
                else if ("ШКІ".equalsIgnoreCase(value)) orderCol = cell.getColumnIndex();
            }

            logger.info("🔍 Знайдені заголовки: {}", foundHeaders);
            logger.info("📌 Індекси колонок - Сума: {}, Замовлення: {}", amountCol, orderCol);

            if (amountCol == -1 || orderCol == -1) {
                throw new IOException("Не всі обов'язкові колонки знайдено у файлі. Потрібні: сума, грн., ШКІ");
            }

            if (registryDateFormatted == null) {
                throw new IOException("Не вдалося визначити дату реєстру з файлу");
            }

            // Починаємо обробку з рядка headerRowNum + 2 (пропускаємо один рядок після заголовків)
            for (int i = headerRowNum + 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String ttn = getCellValueAsString(row.getCell(orderCol));
                if (ttn == null || ttn.isEmpty()) continue;

                Map<String, String> payment = new HashMap<>();
                payment.put("date", registryDateFormatted);
                payment.put("amount", getCellValueAsString(row.getCell(amountCol)));
                payment.put("ttn", ttn);

                payments.add(payment);
            }
        }

        return payments;
    }

    private String formatDate(String yyyyMmDd) {
        String[] parts = yyyyMmDd.split("-");
        if (parts.length != 3) return yyyyMmDd;
        return parts[2] + "." + parts[1] + "." + parts[0];
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

            if (headers.contains("дата") &&
                    headers.contains("сума, грн.") &&
                    headers.contains("ШКІ")) {

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
