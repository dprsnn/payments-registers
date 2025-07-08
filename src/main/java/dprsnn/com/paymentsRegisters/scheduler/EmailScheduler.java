package dprsnn.com.paymentsRegisters.scheduler;

import dprsnn.com.paymentsRegisters.service.GmailImapService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {

    private final GmailImapService emailService;

    public EmailScheduler(GmailImapService emailService) {
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 5 10 * * ?") // Щодня о 10:00 ранку
    public void scheduledEmailProcessing() {
        emailService.readInboxWithAttachments();
    }

    @Scheduled(cron = "0 0 13 * * ?") // Щодня о 13:00 ранку
    public void scheduledEmailProcessingAd() {
        emailService.readInboxWithAttachments();
    }
}
