package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.dto.PaymentRecord;
import dprsnn.com.paymentsRegisters.models.ProcessedEmail;
import dprsnn.com.paymentsRegisters.repos.ProcessedEmailRepository;
import jakarta.mail.*;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GmailImapService {

    @Value("${gmail.imap.host}")
    private String imapHost;

    @Value("${gmail.imap.port}")
    private int imapPort;

    @Value("${gmail.email}")
    private String email;

    @Value("${gmail.app-password}")
    private String appPassword;

    private final ProcessedEmailRepository processedEmailRepository;
    private final CrmOrderService crmOrderService;
    private static final Logger logger = LoggerFactory.getLogger(GmailImapService.class);
    private final TelegramBotNotifier telegramBotNotifier;



    public GmailImapService(ProcessedEmailRepository processedEmailRepository, CrmOrderService crmOrderService, TelegramBotNotifier telegramBotNotifier) {
        this.processedEmailRepository = processedEmailRepository;
        this.crmOrderService = crmOrderService;
        this.telegramBotNotifier = telegramBotNotifier;
        ;
    }
    public void readInboxWithAttachments() {
        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", imapHost);
            props.put("mail.imaps.port", String.valueOf(imapPort));
            props.put("mail.imaps.ssl.enable", "true");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(imapHost, email, appPassword);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();

            for (int i = messages.length - 1; i >= Math.max(0, messages.length - 10); i--) {
                Message message = messages[i];
                String[] messageIdHeader = message.getHeader("Message-ID");

                if (messageIdHeader == null || messageIdHeader.length == 0 ||
                        processedEmailRepository.existsByMessageId(messageIdHeader[0])) {
                    continue;
                }

                String messageId = messageIdHeader[0];
                String subject = message.getSubject();
                String sender = Arrays.toString(message.getFrom());
                Instant sentDate = message.getSentDate() != null ? message.getSentDate().toInstant() : Instant.now();
                Instant processedAt = Instant.now();
                String attachmentPath = "";
                boolean hasAttachments = false;

                logger.info("Новий лист: ID={}, Тема={}, Відправник={}", messageId, subject, sender);

                try {
                    // Отримуємо весь вміст листа
                    String content = getTextFromMessage(message);
//                    logger.info("Вміст листа: {}", content);  // Логування вмісту листа

                    // Обробка листів з посиланнями на завантаження (ФОП)
                    if (subject != null && subject.contains("Реєстр платежів ФОП")) {
                        String downloadUrl = extractDownloadUrl(content);
                        if (downloadUrl != null) {
                            logger.info("Знайдено посилання для завантаження: {}", downloadUrl);
                            Path downloadedFile = downloadFileFromUrl(downloadUrl);

                            if (downloadedFile != null) {
//                                logger.info("Файл завантажено: {}", downloadedFile);
                                try {
                                    List<PaymentRecord> payments = processEvoPayFile(downloadedFile.toFile());
                                    List<Map<String, Object>> results = crmOrderService.makePayments(payments, "evo_pay");
//                                    List<Map<String, Object>> results = new ArrayList<>();

                                    String resultExcelFileName = ExcelGenerator.generateAndSavePaymentsExcel(payments, "evo_pay");
                                    File excelFile = new File("exports/" + resultExcelFileName);

                                    telegramBotNotifier.sendFileToTelegram(excelFile);
                                    logger.info("Файл ЕВО оброблено: {} платежів", payments.size());
                                } catch (Exception e) {
                                    logger.error("Помилка при обробці файлу ЕВО: {}", e.getMessage(), e);
                                } finally {
                                    Files.deleteIfExists(downloadedFile);
                                }
                            }
                        }
                        hasAttachments = true;
                    }

                    // Інша обробка вкладених файлів
                    else if (message.getContent() instanceof Multipart) {
                        Multipart multipart = (Multipart) message.getContent();

                        for (int j = 0; j < multipart.getCount(); j++) {
                            BodyPart bodyPart = multipart.getBodyPart(j);

                            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                                String fileName = bodyPart.getFileName();

                                if (fileName != null && (fileName.toLowerCase().endsWith(".xlsx") ||
                                        fileName.toLowerCase().endsWith(".xls"))) {

                                    Path tempFile = Files.createTempFile("email_attachment_", "_" + fileName);
                                    try (InputStream is = bodyPart.getInputStream()) {
                                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                                        logger.info("Завантажено вкладення: {}, розмір: {} bytes",
                                                fileName, Files.size(tempFile));

                                        if (subject != null) {
                                            if (subject.contains("Реєстр згрупованого платіжного доручення")) {
                                                try {
                                                    List<PaymentRecord> payments = processUkrPostFile(tempFile.toFile());
                                                    List<Map<String, Object>> results = crmOrderService.makePayments(payments, "ukrpost");
//                                                    List<Map<String, Object>> results = new ArrayList<>();
                                                    String resultExcelFileName = ExcelGenerator.generateAndSavePaymentsExcel(payments, "ukrpost");
                                                    File excelFile = new File("exports/" + resultExcelFileName);
                                                    telegramBotNotifier.sendFileToTelegram(excelFile);
                                                    logger.info("Оплати проведено: {} записів, Excel створено та надіслано", results.size());
                                                } catch (Exception e) {
                                                    logger.error("Помилка при обробці реєстру Укрпошти: {}", e.getMessage(), e);
                                                }
                                            } else if (subject.contains("Реєстр платежів контрагента Ільніцький")) {
                                                try {
                                                    // Створюємо папку для зберігання оригіналів, якщо її немає
                                                    Path originalDir = Paths.get("originals");
                                                    if (!Files.exists(originalDir)) {
                                                        Files.createDirectories(originalDir);
                                                    }

                                                    // Зберігаємо оригінальний файл
                                                    Path originalFile = originalDir.resolve(fileName);
                                                    try (InputStream inputStream = bodyPart.getInputStream()) {  // Змінив назву змінної з is на inputStream
                                                        Files.copy(inputStream, originalFile, StandardCopyOption.REPLACE_EXISTING);

                                                        // Обробляємо файл
                                                        List<PaymentRecord> payments = processNovaPayFile(originalFile.toFile());
                                                        List<Map<String, Object>> results = crmOrderService.makePayments(payments, "nova_pay");
//                                                        List<Map<String, Object>> results = new ArrayList<>();

                                                        logger.info("Файл NovaPay Ільніцький успішно оброблено: {} платежів", payments.size());

                                                        // Надсилаємо оригінальний файл до Telegram
                                                        telegramBotNotifier.sendFileToTelegram(originalFile.toFile());
                                                        logger.info("Оригінальний файл Ільніцький NovaPay надіслано до Telegram: {}", fileName);

                                                    }
                                                } catch (Exception e) {
                                                    logger.error("Помилка при обробці файлу NovaPay Ільніцький: {}", e.getMessage(), e);
                                                }
                                            }
                                            else if (subject.contains("Реєстр платежів контрагента Єрмак")) {
                                                try {
                                                    // Створюємо папку для зберігання оригіналів, якщо її немає
                                                    Path originalDir = Paths.get("originals");
                                                    if (!Files.exists(originalDir)) {
                                                        Files.createDirectories(originalDir);
                                                    }

                                                    // Зберігаємо оригінальний файл
                                                    Path originalFile = originalDir.resolve(fileName);
                                                    try (InputStream inputStream = bodyPart.getInputStream()) {  // Змінив назву змінної з is на inputStream
                                                        Files.copy(inputStream, originalFile, StandardCopyOption.REPLACE_EXISTING);

                                                        // Обробляємо файл
                                                        List<PaymentRecord> payments = processNovaPayFile(originalFile.toFile());
                                                        List<Map<String, Object>> results = crmOrderService.makePayments(payments, "nova_pay_swell");
//                                                        List<Map<String, Object>> results = new ArrayList<>();

                                                        logger.info("Файл NovaPay Єрмак успішно оброблено: {} платежів", payments.size());

                                                        // Надсилаємо оригінальний файл до Telegram
                                                        telegramBotNotifier.sendFileToTelegram(originalFile.toFile());
                                                        logger.info("Оригінальний файл Єрмак NovaPay надіслано до Telegram: {}", fileName);

                                                    }
                                                } catch (Exception e) {
                                                    logger.error("Помилка при обробці файлу NovaPay Єрмак: {}", e.getMessage(), e);
                                                }
                                            }
                                        }
                                    } finally {
                                        Files.deleteIfExists(tempFile);
                                    }
                                    hasAttachments = true;
                                }
                            }
                        }
                    }

                    // Зберігаємо як оброблений лист
                    processedEmailRepository.save(new ProcessedEmail(
                            messageId, subject, sender, sentDate, processedAt, attachmentPath, hasAttachments));
                } catch (Exception e) {
                    logger.error("Помилка при обробці листа ID={}: {}", messageId, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Помилка підключення до поштової скриньки: {}", e.getMessage(), e);
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                logger.warn("Не вдалося закрити з'єднання з поштовою скринькою: {}", e.getMessage());
            }
        }
    }



// Допоміжні методи для обробки посилань

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            return (String) message.getContent(); // Якщо це HTML контент
        } else if (message.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.getContent() instanceof String) {
                    return (String) bodyPart.getContent();
                }
            }
        }
        return "";
    }



    private String extractDownloadUrl(String content) {
        logger.debug("Початок витягування URL з вмісту листа");

        if (content == null) {
            logger.warn("Вміст листа є null");
            return null;
        }

        try {
            logger.trace("Пошук URL у вмісті:\n{}", content);

            // Оптимізований regex для URL Google Storage з .xlsx
            Pattern pattern = Pattern.compile("(https?://storage\\.googleapis\\.com/[^\\s<>\"']+\\.xlsx(?:\\?[^\\s<>\"']*)?)");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String url = matcher.group(1);
                url = url.replaceAll("[<>\"']+$", ""); // Видаляємо закриваючі теги/лапки

                logger.info("Знайдено URL для завантаження: {}", url);
                logger.debug("Повний знайдений URL: {}", url);

                return url;
            } else {
                logger.warn("URL не знайдено у вмісті листа");
                // Додатковий аналіз вмісту для debug
                if (content.contains("Завантажити реєстр")) {
                    logger.debug("Знайдено текст 'Завантажити реєстр', але URL не витягнуто");
                }
                if (content.contains("storage.googleapis.com")) {
                    logger.debug("Знайдено 'storage.googleapis.com', але повний URL не витягнуто");
                }
            }
        } catch (Exception e) {
            logger.error("Помилка при витягуванні URL: {}", e.getMessage(), e);
        }

        return null;
    }

    private Path downloadFileFromUrl(String urlString) throws IOException {
        logger.info("Спроба завантаження файлу з URL: {}", urlString);

        HttpURLConnection connection = null;
        try {
            // Декодуємо URL для коректного відображення в логах
            String decodedUrl = URLDecoder.decode(urlString, StandardCharsets.UTF_8.name());
            logger.debug("Декодований URL: {}", decodedUrl);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            logger.debug("Встановлені таймаути: connect={}ms, read={}ms",
                    connection.getConnectTimeout(), connection.getReadTimeout());

            // Логуємо заголовки запиту
            logger.trace("Заголовки запиту:");
            connection.getRequestProperties().forEach((k, v) ->
                    logger.trace("{}: {}", k, v));

            int responseCode = connection.getResponseCode();
            logger.info("HTTP відповідь: {}", responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorMsg = "Помилка HTTP: " + responseCode + " - " + connection.getResponseMessage();
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Обробка Content-Disposition для імені файлу
            String fileName = "download.xlsx";
            String contentDisposition = connection.getHeaderField("Content-Disposition");
            logger.debug("Content-Disposition: {}", contentDisposition);

            if (contentDisposition != null) {
                Pattern pattern = Pattern.compile("filename\\*?=\"?([^\"]+)\"?");
                Matcher matcher = pattern.matcher(contentDisposition);
                if (matcher.find()) {
                    fileName = matcher.group(1);
                    logger.debug("Ім'я файлу з Content-Disposition: {}", fileName);
                }
            } else {
                // Вилучення імені файлу з URL
                fileName = decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1).split("\\?")[0];
                logger.debug("Ім'я файлу з URL: {}", fileName);
            }

            Path tempFile = Files.createTempFile("download_", "_" + fileName);
            logger.info("Тимчасовий файл: {}", tempFile);

            // Логуємо заголовки відповіді
            logger.debug("Заголовки відповіді:");
            connection.getHeaderFields().forEach((k, v) ->
                    logger.debug("{}: {}", k, v));

            // Завантаження файлу
            try (InputStream in = connection.getInputStream()) {
                long bytesCopied = Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Файл успішно завантажено. Розмір: {} байт", bytesCopied);

                // Додаткова перевірка файлу
                if (Files.size(tempFile) == 0) {
                    logger.error("Завантажено порожній файл!");
                    Files.deleteIfExists(tempFile);
                    throw new IOException("Завантажено порожній файл");
                }

                return tempFile;
            }
        } catch (Exception e) {
            logger.error("Помилка завантаження файлу: {}", e.getMessage(), e);

            // Додаткова інформація про помилку
            if (connection != null) {
                try {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        logger.error("Відповідь сервера про помилку: {}", errorResponse);
                    }
                } catch (IOException ioException) {
                    logger.error("Не вдалося прочитати потік помилок", ioException);
                }
            }

            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
                logger.debug("З'єднання закрито");
            }
        }
    }

    private List<PaymentRecord> processEvoPayFile(File file) throws IOException {
        List<PaymentRecord> paymentRecords = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            int headerRowNum = findHeaderRowEvo(sheet);
            if (headerRowNum == -1) {
                throw new IOException("Не вдалося знайти рядок з заголовками");
            }

            Row headerRow = sheet.getRow(headerRowNum);

            int dateCol = -1, amountCol = -1, feeCol = -1, orderCol = -1;

            // Шукаємо потрібні стовпці
            for (Cell cell : headerRow) {
                String cellValue = getCellValueAsString(cell).trim();

                if ("Дата перерахування".equals(cellValue)) dateCol = cell.getColumnIndex();
                else if ("Сума платежу".equals(cellValue)) amountCol = cell.getColumnIndex();
                else if ("Сума комісії з отримувача".equals(cellValue)) feeCol = cell.getColumnIndex();
                else if ("№ замовлення".equals(cellValue)) orderCol = cell.getColumnIndex();
            }

            if (dateCol == -1 || amountCol == -1 || feeCol == -1 || orderCol == -1) {
                throw new IOException("Не вдалося знайти всі необхідні стовпці у файлі");
            }

            // Обробка даних з рядків файлу
            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Зчитуємо значення зі стовпців
                String orderId = parseOrderIdNova(row.getCell(orderCol));
                if (orderId == null || orderId.isEmpty()) {
                    continue; // Пропускаємо рядки з порожнім або невалідним айді
                }

                String date = getCellValueAsString(row.getCell(dateCol));
                String amount = getCellValueAsString(row.getCell(amountCol));
                String rawFee = getCellValueAsString(row.getCell(feeCol)).replace(",", ".").trim();
                if (rawFee.startsWith("-")) {
                    rawFee = rawFee.substring(1); // або: rawFee = String.valueOf(Math.abs(Double.parseDouble(rawFee)));
                }

                // Створюємо PaymentRecord
                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setPaymentDate(date);
                paymentRecord.setAmount(amount);
                paymentRecord.setExpense(rawFee);
                paymentRecord.setSourceOrderId(orderId);
                paymentRecord.setTypeOfPayment("evo_pay");

                paymentRecords.add(paymentRecord);
            }
        }
        return paymentRecords;
    }

    private List<PaymentRecord> processNovaPayFile(File file) throws IOException {
        List<PaymentRecord> paymentRecords = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            int headerRowNum = findHeaderRowNova(sheet);
            if (headerRowNum == -1) {
                throw new IOException("Не вдалося знайти рядок з заголовками");
            }

            Row headerRow = sheet.getRow(headerRowNum);

            int dateCol = -1, amountCol = -1, feeCol = -1, orderCol = -1;

            // Шукаємо потрібні стовпці
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

            // Обробка даних з рядків файлу
            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Зчитуємо значення зі стовпців
                String orderId = parseOrderIdNova(row.getCell(orderCol));
                if (orderId == null || orderId.isEmpty()) {
                    continue; // Пропускаємо рядки з порожнім або невалідним айді
                }

                String date = getCellValueAsString(row.getCell(dateCol));
                String amount = getCellValueAsString(row.getCell(amountCol));
                String fee = getCellValueAsString(row.getCell(feeCol));

                // Створюємо PaymentRecord
                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setPaymentDate(date);
                paymentRecord.setAmount(amount);
                paymentRecord.setExpense(fee);
                paymentRecord.setOrderId(orderId);
                paymentRecord.setTypeOfPayment("nova_pay");

                paymentRecords.add(paymentRecord);
            }
        }
        return paymentRecords;
    }


    private List<PaymentRecord> processUkrPostFile(File file) throws IOException {
        List<PaymentRecord> paymentRecords = new ArrayList<>();
        String registryDateFormatted = null;

        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

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

            int headerRowNum = findHeaderRowUkrPost(sheet);
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

            System.out.println("Знайдені заголовки: " + foundHeaders);
            System.out.println("Індекси колонок - Сума: " + amountCol + ", Замовлення: " + orderCol);

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

                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setPaymentDate(registryDateFormatted);
                paymentRecord.setAmount(getCellValueAsString(row.getCell(amountCol)));
                paymentRecord.setTtn(ttn);
                // Встановлюємо тип платежу для Укрпошти
                paymentRecord.setTypeOfPayment("ukrpost");

                paymentRecords.add(paymentRecord);
            }
        }

        System.out.println("Успішно оброблено " + paymentRecords.size() + " платежів Укрпошти");
        return paymentRecords;
    }

    // Допоміжні методи (такі самі як у processUkrpostExcel)
    private String formatDate(String yyyyMmDd) {
        String[] parts = yyyyMmDd.split("-");
        if (parts.length != 3) return yyyyMmDd;
        return parts[2] + "." + parts[1] + "." + parts[0];
    }

    private int findHeaderRowUkrPost(Sheet sheet) {
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
                System.out.println("Заголовки знайдено в рядку " + i + ": " + headers);
                return i;
            }
        }
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
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (int) numericValue) {
                    return String.valueOf((int) numericValue);
                } else {
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

    private String parseOrderIdNova(Cell cell) {
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

    private int findHeaderRowNova(Sheet sheet) {
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
    private int findHeaderRowEvo(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            List<String> headers = new ArrayList<>();
            for (Cell cell : row) {
                String value = normalizeHeader(getCellValueAsString(cell));
                headers.add(value);
            }

            if (headers.contains("Дата перерахування") &&
                    headers.contains("Сума платежу") &&
                    headers.contains("Сума комісії з отримувача") &&
                    headers.contains("№ замовлення")) {

                logger.info("✅ Заголовки знайдено в рядку {}: {}", i, headers);
                return i;
            }

            logger.debug("⛔ Рядок {} не містить усіх потрібних заголовків: {}", i, headers);
        }

        logger.warn("⚠️ Заголовки не знайдено у жодному рядку.");
        return -1;
    }
}