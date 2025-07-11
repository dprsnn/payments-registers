package dprsnn.com.paymentsRegisters.scheduler;

import dprsnn.com.paymentsRegisters.models.Log;
import dprsnn.com.paymentsRegisters.repos.LogRepo;
import dprsnn.com.paymentsRegisters.service.GmailImapService;
import dprsnn.com.paymentsRegisters.service.UkrpostService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class Scheduler {

    private final GmailImapService emailService;
    private final UkrpostService ukrpostService;
    private final LogRepo logRepo;

    public Scheduler(GmailImapService emailService, UkrpostService ukrpostService, LogRepo logRepo) {
        this.emailService = emailService;
        this.ukrpostService = ukrpostService;
        this.logRepo = logRepo;
    }

    @Scheduled(cron = "0 0 10 * * ?") // Щодня о 10:00 ранку
    public void scheduledEmailProcessing() {
        emailService.readInboxWithAttachments();
    }

    @Scheduled(cron = "0 0 1 * * ?") // Запускати щодня о 01:00 ночі
    public void scheduledUpdateOrderList() {
        System.out.println("Запуск автоматичної синхронізації УкрПошти: " + LocalDateTime.now());
        try {
            ukrpostService.updateOrderList(LocalDate.now(), LocalDate.now().minusDays(1));
            logRepo.save(new Log("Автоматична синхронізація УкрПошти успішна", LocalDateTime.now()));
        } catch (Exception e) {
            logRepo.save(new Log("Помилка автоматичної синхронізації УкрПошти: " + e.getMessage(), LocalDateTime.now()));
            System.err.println("Помилка при автоматичній синхронізації: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
