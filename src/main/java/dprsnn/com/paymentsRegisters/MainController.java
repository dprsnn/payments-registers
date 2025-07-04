package dprsnn.com.paymentsRegisters;

import dprsnn.com.paymentsRegisters.repos.CrmCredentialsRepo;
import dprsnn.com.paymentsRegisters.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {



    @GetMapping("/")
    public String getMainPage(){
        return "index";
    }
}
