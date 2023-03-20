package mz.org.fgh.hl7.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.service.Hl7Service;
import mz.org.fgh.hl7.service.LocationService;
import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.Hl7FileForm;

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

    @GetMapping
    public String findAll(Model model) {
        model.addAttribute("hl7FileList", hl7Service.findAll());
        return "hl7";
    }

    @GetMapping("/{filename:^" + Hl7FileForm.FILENAME_CHARS + "*\\.hl7$}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String filename) {
        ByteArrayResource resource = new ByteArrayResource(hl7Service.read(filename));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{filename:^" + Hl7FileForm.FILENAME_CHARS + "*\\.hl7$}")
    public String delete(@PathVariable String filename, RedirectAttributes redirectAttrs) {
        try {
            hl7Service.delete(filename);
            redirectAttrs.addFlashAttribute(Alert.success("hl7.files.deleted"));
        } catch (AppException e) {
            LOG.error(e.getMessage(), e);
            redirectAttrs.addFlashAttribute(Alert.danger(e.getLocalizedMessage()));
        } finally {
            return "redirect:/hl7";
        }
    }

    @GetMapping("/new")
    public String newHL7Form(
            Hl7FileForm hl7FileForm,
            Model model,
            RedirectAttributes redirectAttrs) {

        try {
            setAllProvinces(hl7FileForm, model);
        } catch (AppException e) {
            LOG.error(e.getMessage(), e);
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
            return "redirect:/hl7";
        }
        return "newHL7";
    }

    @PostMapping("/new")
    public String createHL7(
            @Valid Hl7FileForm hl7FileForm,
            BindingResult result,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) throws HL7Exception, IOException {

        if (result.hasErrors()) {
            return newHL7Form(hl7FileForm, model, redirectAttrs);
        }

        try {

            hl7Service.validateCreate(hl7FileForm.getFilename());

        } catch (AppException e) {
            LOG.error(e.getMessage(), e);
            if (e.getMessage().equals("hl7.create.error.exists")) {
                result.rejectValue("filename", e.getMessage());
            } else {
                model.addAttribute(Alert.danger(e.getMessage()));
            }
            return newHL7Form(hl7FileForm, model, redirectAttrs);
        }

        hl7Service.create(hl7FileForm.getFilename(), hl7FileForm.getHealthFacilities());

        redirectAttrs.addFlashAttribute(Alert.success("hl7.schedule.success"));
        return "redirect:/hl7";
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
