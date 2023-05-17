package mz.org.fgh.hl7.web.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Future;

import javax.validation.Valid;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.service.Hl7Service;
import mz.org.fgh.hl7.service.LocationService;
import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.SearchForm;

@Controller
@RequestMapping("/hl7")
public class HL7Controller {

    private static final Logger LOG = LoggerFactory.getLogger(HL7Controller.class);

    private static final int ROW_SIZE = 5;

    private LocationService locationService;

    private Hl7Service hl7Service;

    public HL7Controller(LocationService locationService, Hl7Service hl7Service) {
        this.locationService = locationService;
        this.hl7Service = hl7Service;
    }

    @ModelAttribute("searchAvailable")
    public boolean isSearchAvailable() {
        return hl7Service.isSearchAvailable();
    }

    @ModelAttribute("needsNewFile")
    public boolean needsNewFile() {
        return hl7Service.getHl7File() == null
                && hl7Service.getPreviousHl7File() == null;
    }

    @ModelAttribute("previousLastModifiedTime")
    public LocalDateTime getPreviousLastModifiedTime() {
        HL7File previousHl7File = hl7Service.getPreviousHl7File();
        return previousHl7File != null ? previousHl7File.getLastModifiedTime() : null;
    }

    @ModelAttribute("hl7File")
    public Future<HL7File> getHl7File() {
        return hl7Service.getHl7File();
    }

    @GetMapping("/config")
    public String newHL7Form(
            Hl7FileForm hl7FileForm,
            Model model,
            RedirectAttributes redirectAttrs) {

        try {
            setAllProvinces(hl7FileForm, model);
        } catch (AppException e) {
            LOG.error(e.getMessage(), e);
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
            return "redirect:/hl7/search";
        }
        return "config";
    }

    @PostMapping("/config")
    public String createHL7(
            @Valid Hl7FileForm hl7FileForm,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttrs) throws HL7Exception, IOException {

        if (result.hasErrors()) {
            return newHL7Form(hl7FileForm, model, redirectAttrs);
        }

        hl7Service.generateHl7File(hl7FileForm.getHealthFacilities());

        redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));
        return "redirect:/hl7/search";
    }

    @GetMapping("/search")
    public String search(@Valid SearchForm searchForm,
            BindingResult bindingResult,
            Model model) throws FileNotFoundException {

        try {

            if (bindingResult.hasErrors() || StringUtils.isEmpty(searchForm.getPartialNid())) {
                return "search";
            }

            List<PatientDemographic> search = hl7Service.search(searchForm.getPartialNid());

            if (search.isEmpty()) {
                model.addAttribute("errorMessage",
                        "Não encontramos nenhum item que corresponda à sua consulta de pesquisa.");
            } else {
                model.addAttribute("hl7Patients", search);
            }

        } catch (AppException e) {
            model.addAttribute(Alert.danger(e.getMessage()));
        }

        return "search";

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
