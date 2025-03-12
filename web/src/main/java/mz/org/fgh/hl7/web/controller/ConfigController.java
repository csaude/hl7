package mz.org.fgh.hl7.web.controller;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import mz.org.fgh.hl7.web.service.SchedulerConfigService;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.AppException;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.HL7FileRequest;
import mz.org.fgh.hl7.web.model.Location;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import mz.org.fgh.hl7.web.service.Hl7Service;
import mz.org.fgh.hl7.web.service.LocationService;

@Controller
@RequestMapping("/config")
public class ConfigController {
    private static final int ROW_SIZE = 5;

    private static final Logger LOG = LoggerFactory.getLogger(ConfigController.class);

    private Hl7Service hl7Service;
    private LocationService locationService;
    private SchedulerConfigService schedulerConfigService;
    private WebClient webClient;

    public ConfigController(Hl7Service hl7Service, LocationService locationService, SchedulerConfigService schedulerConfigService, WebClient webClient) {
        this.hl7Service = hl7Service;
        this.locationService = locationService;
        this.schedulerConfigService = schedulerConfigService;
        this.webClient = webClient;
    }

    @GetMapping
    public String newHL7Form(
            Hl7FileForm hl7FileForm,
            Model model,
            RedirectAttributes redirectAttrs) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOG.debug("Authentication: {}", authentication);
        if (authentication != null) {
            LOG.debug("Principal: {}", authentication.getPrincipal());
        }

        try {
            CompletableFuture<ProcessingResult> hl7FileProcessing = hl7Service.getProcessingResult();
            if (hl7FileProcessing != null && !hl7FileProcessing.isDone()) {
                throw new AppException(
                        "hl7.files.processing.error.previous");
            }

            HL7File hl7File = hl7Service.getHl7File();

            if (hl7File != null
                    && nullOrEquals(hl7FileForm.getProvince(), hl7File.getProvince())
                    && nullOrEquals(hl7FileForm.getDistrict(), hl7File.getDistrict())) {
                hl7FileForm.setProvince(hl7File.getProvince());
                hl7FileForm.setDistrict(hl7File.getDistrict());
                hl7FileForm.setHealthFacilities(hl7File.getHealthFacilities());
            }

            setAllProvinces(hl7FileForm, model);

            int frequency = schedulerConfigService.getFrequency();
            LocalTime generationTime = schedulerConfigService.getGenerationTime();

            model.addAttribute("frequency", frequency);
            model.addAttribute("generationTime", generationTime);


        } catch (AppException e) {
            LOG.error(e.getMessage(), e);
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
            return "redirect:/search";
        }
        return "config";
    }

    @PostMapping
    public String createHL7(
            @Valid Hl7FileForm hl7FileForm,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttrs,
            HttpSession session ) throws HL7Exception, IOException {

        if (result.hasErrors()) {
            return newHL7Form(hl7FileForm, model, redirectAttrs);
        }

        HL7File hl7File = hl7Service.getHl7File();
        hl7File.setProvince(hl7FileForm.getProvince());
        hl7File.setDistrict(hl7FileForm.getDistrict());
        hl7File.setHealthFacilities(hl7FileForm.getHealthFacilities());

        // Get JobId from the scheduledTask
        String jobId = schedulerConfigService.scheduledTask(hl7FileForm);

        if(!Objects.equals(jobId, "PROCESSING")){
            redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));

        }
        else {
            redirectAttrs.addFlashAttribute(Alert.warning("hl7.processing.warning"));
        }

        return "redirect:/search";
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

    private boolean nullOrEquals(Location l1, Location l2) {
        return l1 == null || l1.equals(l2);
    }

}
