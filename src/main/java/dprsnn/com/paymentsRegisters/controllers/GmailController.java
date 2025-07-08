package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.service.GmailImapService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GmailController {
    private final GmailImapService gmailImapService;

    public GmailController(GmailImapService gmailImapService) {
        this.gmailImapService = gmailImapService;
    }

    @GetMapping("/gmail")
    public String readGmail(){
        gmailImapService.readInboxWithAttachments();
        return "redirect:/";
    }
}
