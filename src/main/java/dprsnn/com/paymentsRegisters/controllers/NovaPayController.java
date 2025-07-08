package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import dprsnn.com.paymentsRegisters.service.CrmOrderService;
import dprsnn.com.paymentsRegisters.service.ExcelGenerator;
import dprsnn.com.paymentsRegisters.service.NovaPayService;
import dprsnn.com.paymentsRegisters.service.TelegramBotNotifier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class NovaPayController {

    private static final Logger logger = LoggerFactory.getLogger(NovaPayController.class);
    private final CrmOrderService CrmOrderService;
    private final CrmOrderService crmOrderService;
    private final TelegramBotNotifier telegramBotNotifier;

    public NovaPayController(CrmOrderService CrmOrderService, CrmOrderService crmOrderService, TelegramBotNotifier telegramBotNotifier) {
        this.CrmOrderService = CrmOrderService;
        this.crmOrderService = crmOrderService;
        this.telegramBotNotifier = telegramBotNotifier;
    }

    @GetMapping("/nova-pay")
    public String showUploadForm() {
        return "nova_pay";
    }

    @PostMapping(value = "/nova-pay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("paymentType") String paymentType) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", "Будь ласка, виберіть файл для завантаження."
                    ));
        }

        try {
            List<Map<String, String>> paymentData = new ArrayList<>();

            if ("nova_pay".equals(paymentType)) {
                paymentData = processNovaPayExcel(file);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "message", "Невідомий тип оплати: " + paymentType
                        ));
            }

            logger.info("Оброблено {} платежів з файлу {}", paymentData.size(), file.getOriginalFilename());

            return ResponseEntity.ok()
                    .body(Map.of(
                            "status", "success",
                            "message", "Файл успішно оброблено. Знайдено " + paymentData.size() + " платежів.",
                            "payments", paymentData
                    ));

        } catch (IOException e) {
            logger.error("Помилка при обробці файлу: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", "Помилка при обробці файлу: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/nova-pay/make-payments")
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


    public List<Map<String, String>> processNovaPayExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> payments = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int headerRowNum = findHeaderRow(sheet);
            if (headerRowNum == -1) {
                throw new IOException("Не вдалося знайти рядок з заголовками");
            }

            Row headerRow = sheet.getRow(headerRowNum);

            int dateCol = -1, amountCol = -1, feeCol = -1, orderCol = -1;

            for (Cell cell : headerRow) {
                String cellValue = getCellValueAsString(cell).trim();

                if ("Дата перерахунку коштів".equals(cellValue)) dateCol = cell.getColumnIndex();
                else if ("Сума принятих коштів".equals(cellValue)) amountCol = cell.getColumnIndex();
                else if ("Сума утриманої винагороди".equals(cellValue)) feeCol = cell.getColumnIndex();
                else if ("Номер замовлення".equals(cellValue)) orderCol = cell.getColumnIndex();
            }

            if (dateCol == -1 || amountCol == -1 || feeCol == -1 || orderCol == -1) {
                throw new IOException("Не вдалося знайти всі необхідні стовпці у файлі");
            }

            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderId = parseOrderId(row.getCell(orderCol));
                if (orderId == null || orderId.isEmpty()) {
                    continue; // Пропускаємо рядки з пустим або невалідним айді
                }

                String date = getCellValueAsString(row.getCell(dateCol));
                String amount = getCellValueAsString(row.getCell(amountCol));
                String fee = getCellValueAsString(row.getCell(feeCol));

                Map<String, String> payment = new HashMap<>();
                payment.put("date", date);
                payment.put("amount", amount);
                payment.put("fee", fee);
                payment.put("order", orderId);

                payments.add(payment);
            }
        }
        return payments;
    }

    private String parseOrderId(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                String stringValue = cell.getStringCellValue().trim();
                try {
                    // Спроба перетворити рядок на ціле число
                    return String.valueOf(Integer.parseInt(stringValue));
                } catch (NumberFormatException e) {
                    // Якщо не число - повертаємо null (ігноруємо цей рядок)
                    return null;
                }

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return null; // Ігноруємо дати
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return null; // Ігноруємо дробові числа

            case FORMULA:
                try {
                    double formulaValue = cell.getNumericCellValue();
                    if (formulaValue == Math.floor(formulaValue) && !Double.isInfinite(formulaValue)) {
                        return String.valueOf((int) formulaValue);
                    }
                } catch (IllegalStateException e) {
                    // Якщо формула повертає не число
                }
                return null;

            default:
                return null; // Ігноруємо всі інші типи
        }
    }

    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (Cell cell : row) {
                if ("Дата перерахунку коштів".equals(getCellValueAsString(cell).trim())) {
                    return i;
                }
            }
        }
        return -1;
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
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}