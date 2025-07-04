package dprsnn.com.paymentsRegisters.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // без .html — Spring сам знайде login.html
    }
}