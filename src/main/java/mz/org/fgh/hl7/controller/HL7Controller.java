package mz.org.fgh.hl7.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import mz.org.fgh.hl7.Hl7FileForm;
import mz.org.fgh.hl7.Location;
import mz.org.fgh.hl7.service.LocationService;

@Controller
@RequestMapping("/hl7")
public class HL7Controller {

    private static final String ZAMBEZIA = "770acffb-cfeb-46dc-92b0-9d6400f851b9";

    private LocationService locationService;

    public HL7Controller(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String showForm(
            @RequestParam(name = "district", required = false) Long districtId,
            Hl7FileForm hl7FileForm,
            Model model) {

        Location province = locationService.findByUuid(ZAMBEZIA);
        hl7FileForm.setProvince(province);
        if (hl7FileForm.getDistrict() == null) {
            hl7FileForm.setDistrict(province.getChildLocations().get(0));
        }
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
