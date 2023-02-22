package mz.org.fgh.hl7.controller;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    public String showHl7Form() {

        // @SuppressWarnings("unchecked")
        // List<Location> allProvinces = (List<Location>) model.getAttribute("allProvinces");
        // Location province = hl7FileForm.getProvince();
        // if (province == null) {
        //     province = allProvinces.get(0);
        //     hl7FileForm.setProvince(province);
        // }

        // if (hl7FileForm.getDistrict() == null) {
        //     hl7FileForm.setDistrict(province.getChildLocations().get(0));
        // }

        // List<Location> healthFacilities = hl7FileForm.getDistrict().getChildLocations();
        // model.addAttribute("partitionedHF", ListUtils.partition(healthFacilities, 5));

        return "hl7";
    }

    @PostMapping
    public String generateHl7Files(@Valid Hl7FileForm hl7FileForm, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "hl7";
        }
        return "redirect:/";
    }

    @ModelAttribute
    private void setAllProvinces(Hl7FileForm hl7FileForm, Model model) {
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
        List<Location> healthFacilities = hl7FileForm.getDistrict().getChildLocations();
        model.addAttribute("partitionedHF", ListUtils.partition(healthFacilities, 5));
    }
}
