package dprsnn.com.paymentsRegisters.scheduler;

import dprsnn.com.paymentsRegisters.models.Log;
import dprsnn.com.paymentsRegisters.repos.LogRepo;
import dprsnn.com.paymentsRegisters.service.GmailImapService;
import dprsnn.com.paymentsRegisters.service.MonobankService;
import dprsnn.com.paymentsRegisters.service.UkrpostService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Component
public class Scheduler {

    private final GmailImapService emailService;
    private final UkrpostService ukrpostService;
    private final LogRepo logRepo;
    private final MonobankService monobankService;

    public Scheduler(GmailImapService emailService, UkrpostService ukrpostService, LogRepo logRepo, MonobankService monobankService) {
        this.emailService = emailService;
        this.ukrpostService = ukrpostService;
        this.logRepo = logRepo;
        this.monobankService = monobankService;
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Kyiv") // Щодня о 10:00 ранку
    public void scheduledEmailProcessing() {
        logRepo.save(new Log("Запуск автоматичного читання листів", LocalDateTime.now()));
        emailService.readInboxWithAttachments();
    }

    @Scheduled(cron = "0 0 1 * * ?", zone = "Europe/Kyiv") // Запускати щодня о 01:00 ночі
    public void scheduledUpdateOrderList() {
        System.out.println("Запуск автоматичної синхронізації УкрПошти: " + LocalDateTime.now());
        try {
            ukrpostService.updateOrderList(LocalDate.now(), LocalDate.now().minusDays(5));
            logRepo.save(new Log("Автоматична синхронізація УкрПошти успішна", LocalDateTime.now()));
        } catch (Exception e) {
            logRepo.save(new Log("Помилка автоматичної синхронізації УкрПошти: " + e.getMessage(), LocalDateTime.now()));
            System.err.println("Помилка при автоматичній синхронізації: " + e.getMessage());
            e.printStackTrace();
        }
    }
//    @Scheduled(cron = "0 30 9 * * ?", zone = "Europe/Kyiv")
//    public void testSchedule() {
//        LocalDate date = LocalDate.now().minusDays(1);
//        monobankService.processPayments(date);
//        logRepo.save(new Log("Автоматичне проведення еквайрингів монобанк", LocalDateTime.now()));
//    }

}
