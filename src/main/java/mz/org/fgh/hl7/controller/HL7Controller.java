package mz.org.fgh.hl7.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import mz.org.fgh.hl7.Hl7FileForm;
import mz.org.fgh.hl7.Location;
import mz.org.fgh.hl7.service.LocationService;

@Controller
@RequestMapping("/hl7")
public class HL7Controller {

    private LocationService locationService;

    public HL7Controller(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String showForm(
            Hl7FileForm hl7FileForm,
            Model model) {

        List<Location> allProvinces = locationService.findAllProvinces();
        Location province = hl7FileForm.getProvince();
        if (province == null) {
            province = allProvinces.get(0);
            hl7FileForm.setProvince(province);
        }

        if (hl7FileForm.getDistrict() == null) {
            hl7FileForm.setDistrict(province.getChildLocations().get(0));
        }

        model.addAttribute("allProvinces", allProvinces);

        return "hl7";
    }

    @PostMapping
    public String generateHl7Files(@Valid Hl7FileForm hl7FileForm, BindingResult result) {
        if (result.hasErrors()) {
            return "hl7";
        }
        return "redirect:/";
    }
}
