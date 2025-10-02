package dprsnn.com.paymentsRegisters.controllers;

import dprsnn.com.paymentsRegisters.models.CrmCredentialsModel;
import dprsnn.com.paymentsRegisters.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("")
    public String getSettingsPage(Model model) {
        model.addAttribute("api_key", settingsService.getApiKey());

        List<CrmCredentialsModel> list = settingsService.loadSettingsPage();

        for (int i = 0; i < list.size(); i++){
            model.addAttribute(list.get(i).getParamName(), list.get(i).getParamValue());
        }
        return "settings";
    }

    @PostMapping("/save-api-key")
    public String saveApiKey(@RequestParam(name = "new_api_key") String newApiKey,
                             RedirectAttributes redirectAttributes) {
        settingsService.setApiKey(newApiKey);

        // Додаємо повідомлення у flash
        redirectAttributes.addFlashAttribute("successMessage", "API Key успішно збережено!");

        return "redirect:/settings";
    }

    @PostMapping("/save-payments")
    public String savePayments(@RequestParam(name = "nova-pay", required = false) String novaPay,
                               @RequestParam(name = "nova-pay-swell", required = false) String novaPaySwell,
                               @RequestParam(name = "evo-pay", required = false) String evoPay,
                               @RequestParam(name = "mono-uht", required = false) String monoUht,
                               @RequestParam(name = "mono-hh", required = false) String monoHh,
                               @RequestParam(name = "ukrpost", required = false) String ukrpost,
                               @RequestParam(name = "mono-swell", required = false) String monoSwell, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage", "ID оплат успішно збережено!");

//        System.out.println(novaPay + " " + monoUht + " " + monoHh + " " + monoSwell);
        settingsService.savePayments(novaPay, novaPaySwell ,monoUht, monoHh, monoSwell, evoPay, ukrpost);

        return "redirect:/settings";

    }

    @PostMapping("/save-other")
    public String saveOtherSettings(@RequestParam(name = "nova-expense-id", required = false) String expenseId,
                                    @RequestParam(name = "status-id", required = false) String statusId,
                                    @RequestParam(name = "custom-field-id", required = false) String customFieldId,
                                    @RequestParam(name = "mono-expense-id", required = false) String monoExpenseId,
                                    @RequestParam(name = "evo-expense-id", required = false) String evoExpenseId,
                                    RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage", "Інші налаштування успішно збережено!");

        // Зберігаємо дані через сервіс
//        System.out.println("====" + monoExpenseId);
        settingsService.saveOtherSettings(expenseId, statusId, customFieldId, monoExpenseId, evoExpenseId);

        return "redirect:/settings";
    }
    @PostMapping("/save-sources")
    public String saveSources(@RequestParam(name = "uht_source", required = false) String uhtSource,
                                    @RequestParam(name = "swell_source", required = false) String swellSource,
                                    @RequestParam(name = "hh_source", required = false) String hhSource,
                                    RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage", "Джерела успішно збережено!");

        // Зберігаємо дані через сервіс
//        System.out.println("====" + monoExpenseId);
        settingsService.saveSources(uhtSource, swellSource, hhSource);

        return "redirect:/settings";
    }

    @PostMapping("/save-mono")
    public String saveMono(@RequestParam(name = "general-mono", required = false) String generalMono,
                              @RequestParam(name = "mono-swell-eq", required = false) String monoSwellEq,
                              @RequestParam(name = "mono-uht-eq", required = false) String monoUhtEq,
                              @RequestParam(name = "mono-hh-eq", required = false) String monoHHEq,
                              RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage", "Налаштування монобанк успішно збережено!");

        // Зберігаємо дані через сервіс
//        System.out.println("====" + monoExpenseId);
        settingsService.saveMonoSettings(generalMono, monoSwellEq, monoUhtEq, monoHHEq);

        return "redirect:/settings";
    }


}
