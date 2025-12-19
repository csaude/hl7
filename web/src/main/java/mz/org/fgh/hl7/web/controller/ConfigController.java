package mz.org.fgh.hl7.web.controller;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.AppException;
import mz.org.fgh.hl7.web.Hl7FileForm;
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

    public ConfigController(Hl7Service hl7Service, LocationService locationService, SchedulerConfigService schedulerConfigService) {
        this.hl7Service = hl7Service;
        this.locationService = locationService;
        this.schedulerConfigService = schedulerConfigService;
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

            if (hl7FileForm.getProvince() == null) {
                Location savedProvince = schedulerConfigService.getProvince();
                if (savedProvince != null) {
                    hl7FileForm.setProvince(savedProvince);
                }
            }
            if (hl7FileForm.getDistrict() == null) {
                Location savedDistrict = schedulerConfigService.getDistrict();
                if (savedDistrict != null) {
                    hl7FileForm.setDistrict(savedDistrict);
                }
            }
            if (hl7FileForm.getHealthFacilities() == null) {
                List<Location> savedFacilities = schedulerConfigService.getHealthFacilitiesList();
                if (savedFacilities != null && !savedFacilities.isEmpty()) {
                    hl7FileForm.setHealthFacilities(savedFacilities);
                }
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
            HttpSession session ) throws Exception {

        if (result.hasErrors()) {
            setAllProvinces(hl7FileForm, model);
            model.addAttribute("frequency", schedulerConfigService.getFrequency());
            model.addAttribute("generationTime", schedulerConfigService.getGenerationTime());
            return "config"; // Stay on the config page
        }

        String jobId;
        try {
            jobId = schedulerConfigService.scheduledTask(hl7FileForm);

        } catch (Exception e) {
            LOG.warn("Failed to schedule task: {}", e.getMessage());

            model.addAttribute("alert", Alert.danger("hl7.generate.error.server.down"));
            redirectAttrs.addFlashAttribute(Alert.danger("hl7.generate.error.server.down"));

            // Re-populate the form and return to the config page
            setAllProvinces(hl7FileForm, model);
            model.addAttribute("frequency", hl7FileForm.getFrequency());
            model.addAttribute("generationTime", hl7FileForm.getGenerationTime());
            return "config"; // Stay on the config page
        }

        redirectAttrs.addFlashAttribute("jobId", jobId);

        if (Objects.equals(jobId, "PROCESSING")) {

            model.addAttribute("alert", Alert.warning("hl7.processing.warning"));
            setAllProvinces(hl7FileForm, model);
            model.addAttribute("frequency", schedulerConfigService.getFrequency());
            model.addAttribute("generationTime", schedulerConfigService.getGenerationTime());
            return "config"; // <-- Stays on config page

        } else {
            redirectAttrs.addFlashAttribute("jobId", jobId);
            redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));
            return "redirect:/search"; // <-- Redirects to search
        }
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
