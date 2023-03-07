package mz.org.fgh.hl7.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.Alert;
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.Hl7FileForm;
import mz.org.fgh.hl7.Location;
import mz.org.fgh.hl7.service.Hl7Service;
import mz.org.fgh.hl7.service.LocationService;
import mz.org.fgh.hl7.util.Util;

@Controller
@RequestMapping("/hl7")
public class HL7Controller {

    private static final int ROW_SIZE = 5;

    private LocationService locationService;
    
    @Value("${app.hl7.folder}") 
    private String hl7LocationFolder;
    
    @Autowired
    private Hl7Service hl7Service;

    public HL7Controller(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String showHl7Form(
            Hl7FileForm hl7FileForm,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        try {

            setAllProvinces(hl7FileForm, model);

            // Store form state so we can rebuild it if anything happens
            session.setAttribute("previousForm", hl7FileForm);

        } catch (AppException e) {
            // Try to rebuild form state
            Hl7FileForm previousForm = (Hl7FileForm) session.getAttribute("previousForm");
            if (previousForm != null) {
                model.addAttribute(Alert.danger(e.getMessage()));
                model.addAttribute("hl7FileForm", previousForm);
                model.addAttribute("allProvinces", Arrays.asList(previousForm.getProvince()));
                model.addAttribute("partitionedHF",
                        ListUtils.partition(previousForm.getDistrict().getChildLocations(), ROW_SIZE));
            } else {
                redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
                return "redirect:/";
            }
        }
        return "hl7";
    }

    @PostMapping
    public String generateHl7Files(
            @Valid Hl7FileForm hl7FileForm,
            BindingResult result,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) throws HL7Exception, IOException {

        if (result.hasErrors()) {
            return showHl7Form(hl7FileForm, model, session, redirectAttrs);
        }
		
        hl7Service.createHl7File(Util.extractUuid(hl7FileForm.getHealthFacilities()), hl7FileForm.getFilename(), hl7LocationFolder);


        redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));
        return "redirect:/";
    }

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
        // Divide health facility list into equal sized sublists
        model.addAttribute("partitionedHF", ListUtils.partition(healthFacilities, ROW_SIZE));
    }
}
