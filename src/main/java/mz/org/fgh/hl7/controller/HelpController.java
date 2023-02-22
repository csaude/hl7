package mz.org.fgh.hl7.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/help")
public class HelpController {

    @Value("${app.version}")
    private String appVersion;

    @GetMapping
    public String showHelpPage(Model model) {
        model.addAttribute("appVersion", appVersion);
        return "help";
    }
}
