package dprsnn.com.paymentsRegisters.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String EXPORTS_DIR = "exports"; // Папка для збереження файлів

    @GetMapping("/files")
    public String showFiles(Model model) {
        File folder = new File(EXPORTS_DIR);
        if (!folder.exists()) {
            folder.mkdir();
        }

        List<String> files = Arrays.stream(folder.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());

        model.addAttribute("files", files);
        return "files"; // Назва сторінки для виведення файлів
    }

    @GetMapping("/files/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("filename") String filename) throws IOException {
        Path filePath = Paths.get(EXPORTS_DIR, filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] fileContent = Files.readAllBytes(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);
        return ResponseEntity.ok().headers(headers).body(fileContent);
    }

    @DeleteMapping("/files/delete/{filename}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable("filename") String filename) {
        Path filePath = Paths.get(EXPORTS_DIR, filename);  // Використовуємо реальне значення filename
        logger.info("Шлях до файлу: {}", filePath.toString()); // Логування шляху до файлу

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Файл видалений"));
            } else {
                logger.error("Файл не знайдений: {}", filePath.toString()); // Логування помилки
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Файл не знайдений"));
            }
        } catch (IOException e) {
            logger.error("Помилка при видаленні файлу: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Помилка при видаленні файлу"));
        }
    }


}
