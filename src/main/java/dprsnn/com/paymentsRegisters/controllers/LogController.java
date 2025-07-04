package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.repos.LogRepo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogController {

    private final LogRepo logRepo;

    public LogController(LogRepo logRepo) {
        this.logRepo = logRepo;
    }

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        model.addAttribute("logs", logRepo.findAll(Sort.by(Sort.Direction.DESC, "logTime")));
        return "logs"; // Назва шаблону без .html
    }
}
