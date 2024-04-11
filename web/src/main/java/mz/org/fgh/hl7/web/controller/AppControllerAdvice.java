package mz.org.fgh.hl7.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppControllerAdvice {

    @Value("${app.version}")
    private String appVersion;

    @ModelAttribute("appVersion")
    public String getAppVersion() {
        return appVersion;
    }

    @ModelAttribute("username")
    public String getUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "";
    }
}
