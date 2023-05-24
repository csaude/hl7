package mz.org.fgh.hl7.web.controller;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Future;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.service.Hl7Service;
import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.SearchForm;

@Controller
@RequestMapping("/search")
public class SearchController {

    private Hl7Service hl7Service;

    public SearchController(Hl7Service hl7Service) {
        this.hl7Service = hl7Service;
    }

    @ModelAttribute("searchAvailable")
    public boolean isSearchAvailable() {
        return hl7Service.isSearchAvailable();
    }

    @ModelAttribute("needsNewFile")
    public boolean needsNewFile() {
        return hl7Service.getHl7FileFuture() == null
                && hl7Service.getHl7File() == null;
    }

    @ModelAttribute("hl7File")
    public HL7File getHl7File() {
        return hl7Service.getHl7File();
    }

    @ModelAttribute("hl7FileFuture")
    public Future<HL7File> getHl7FileFuture() {
        return hl7Service.getHl7FileFuture();
    }

    @ModelAttribute("healthFacilities")
    public String getHealthFacilities() {
        HL7File hl7File = hl7Service.getHl7File();
        if (hl7File == null) {
            return "";
        }
        return Location.joinLocations(hl7File.getHealthFacilities());
    }

    @GetMapping
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
}
