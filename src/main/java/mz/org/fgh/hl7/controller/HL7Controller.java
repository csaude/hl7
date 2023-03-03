package mz.org.fgh.hl7.controller;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mz.org.fgh.hl7.Alert;
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.Hl7FileForm;
import mz.org.fgh.hl7.Location;
import mz.org.fgh.hl7.service.HL7FileService;
import mz.org.fgh.hl7.service.LocationService;

@Controller
@RequestMapping("/hl7")
public class HL7Controller {

    private static final int ROW_SIZE = 5;

    private LocationService locationService;

    private HL7FileService hl7FileService;

    public HL7Controller(LocationService locationService, HL7FileService hl7FileService) {
        this.locationService = locationService;
        this.hl7FileService = hl7FileService;
    }

    @GetMapping
    public String findAll(Model model) {
        model.addAttribute("hl7FileList", hl7FileService.findAll());
        return "hl7";
    }

    @GetMapping("/{filename:^" + Hl7FileForm.FILENAME_CHARS + "*\\.hl7$}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String filename) {
        ByteArrayResource resource = new ByteArrayResource(hl7FileService.read(filename));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{filename:^" + Hl7FileForm.FILENAME_CHARS + "*\\.hl7$}")
    public String delete(@PathVariable String filename, RedirectAttributes redirectAttrs) {
        try {
            hl7FileService.delete(filename);
            redirectAttrs.addFlashAttribute(Alert.success("hl7.files.deleted"));
        } catch (AppException e) {
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
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
            return "redirect:/hl7";
        }
        return "newHL7";
    }

    @PostMapping
    public String createHL7(
            @Valid Hl7FileForm hl7FileForm,
            BindingResult result,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            return newHL7Form(hl7FileForm, model, redirectAttrs);
        }

        hl7FileService.create(hl7FileForm.getFilename());

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
