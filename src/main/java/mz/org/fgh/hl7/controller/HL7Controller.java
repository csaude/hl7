package mz.org.fgh.hl7.controller;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import mz.org.fgh.hl7.Alert;
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
    public String showHl7Form(Hl7FileForm hl7FileForm, Model model) {
        setAllProvinces(hl7FileForm, model);
        return "hl7";
    }

    @PostMapping
    public String generateHl7Files(
        @Valid Hl7FileForm hl7FileForm,
        BindingResult result,
        Model model,
        RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            return showHl7Form(hl7FileForm, model);
        }

        redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));
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
