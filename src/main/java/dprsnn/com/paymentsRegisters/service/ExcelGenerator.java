package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ExcelGenerator {

    private static final String EXPORT_DIR = "exports/";

    public static String generateAndSavePaymentsExcel(List<PaymentRecord> payments, String paymentType) throws IOException {
        // Створюємо папку, якщо її немає
        Files.createDirectories(Paths.get(EXPORT_DIR));

        // Генеруємо унікальне ім'я файлу
        String fileName;
        if (paymentType.equals("evo_pay") || paymentType.equals("nova_pay") || paymentType.equals("ukrpost")){
            fileName = paymentType + payments.getFirst().getFormatedDate() + ".xlsx";
        } else {
            fileName = paymentType + payments.getFirst().getFormatedDatePlusOneDay() + ".xlsx";
        }

        Path filePath = Paths.get(EXPORT_DIR + fileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Payments");

            // Додаємо перший рядок з текстом "123"
            Row firstRow = sheet.createRow(0);

            if(paymentType.equals("mono_pay_uht")){
                firstRow.createCell(0).setCellValue("uht.net.ua виписка за перiод з " + payments.getFirst().getFormatedDatePlusOneDay() + " по " + payments.getFirst().getFormatedDatePlusOneDay());
            } else if(paymentType.equals("mono_pay_hh")){
                firstRow.createCell(0).setCellValue("hh.in.ua виписка за перiод з " + payments.getFirst().getFormatedDatePlusOneDay() + " по " + payments.getFirst().getFormatedDatePlusOneDay());
            } else if(paymentType.equals("mono_pay_swell")){
                firstRow.createCell(0).setCellValue("swell.in.ua виписка за перiод з " + payments.getFirst().getFormatedDatePlusOneDay() + " по " + payments.getFirst().getFormatedDatePlusOneDay());
            } else if(paymentType.equals("evo_pay")){
                firstRow.createCell(0).setCellValue("EVOPAY а за перiод " + payments.getFirst().getFormatedDate());
            } else if(paymentType.equals("ukrpost")){
                firstRow.createCell(0).setCellValue("УкрПошта виписка за " + payments.getFirst().getFormatedDate());
            }

            // Створення заголовків (рядок 1)
            Row headerRow = sheet.createRow(1);

            String[] headers;
            if (paymentType.equals("ukrpost")) {
                headers = new String[]{"ТТН", "Сума", "Комісія банку", "Дата платежу", "Айді замовлення СРМ"};
            } else {
                headers = new String[]{"Номер замовлення джерела", "Сума", "Комісія банку", "Дата платежу", "Айді замовлення СРМ"};
            }
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Заповнення даних і підсумків
            int rowNum = 2;
            double totalAmount = 0.0;
            double totalExpense = 0.0;

            for (PaymentRecord payment : payments) {
                Row row = sheet.createRow(rowNum++);

                if(paymentType.equals("ukrpost")){
                    row.createCell(0).setCellValue(payment.getTtn());
                } else{
                    row.createCell(0).setCellValue(payment.getSourceOrderId());
                }

                double amount = parseDouble(payment.getAmount());
                double expense = parseDouble(payment.getExpense());

                row.createCell(1).setCellValue(amount);
                row.createCell(2).setCellValue(expense);

                if (paymentType.equals("evo_pay") || paymentType.equals("nova_pay") || paymentType.equals("ukrpost")) {
                    row.createCell(3).setCellValue(payment.getFormatedDate());
                } else {
                    row.createCell(3).setCellValue(payment.getFormatedDatePlusOneDay());
                }

                row.createCell(4).setCellValue(payment.getOrderId());

                totalAmount += amount;
                totalExpense += expense;
            }

            // Рядок підсумків
            // Створюємо стиль з сірим фоном
            CellStyle grayBackgroundStyle = workbook.createCellStyle();
            grayBackgroundStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            grayBackgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

// Жирний шрифт для підсумків
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            grayBackgroundStyle.setFont(boldFont);

// Створюємо рядок і застосовуємо стиль
            Row totalRow = sheet.createRow(rowNum);

            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("ЗАГАЛОМ:");
            totalLabelCell.setCellStyle(grayBackgroundStyle);

            Cell totalAmountCell = totalRow.createCell(1);
            totalAmountCell.setCellValue(totalAmount);
            totalAmountCell.setCellStyle(grayBackgroundStyle);

            Cell totalExpenseCell = totalRow.createCell(2);
            totalExpenseCell.setCellValue(totalExpense);
            totalExpenseCell.setCellStyle(grayBackgroundStyle);


            // Автопідбір ширини стовпців
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }


            // Зберігаємо файл
            try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                workbook.write(outputStream);
            }
        }

        return fileName;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

}