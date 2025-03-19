package mz.org.fgh.hl7.web.controller;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import mz.org.fgh.hl7.web.model.*;
import mz.org.fgh.hl7.web.service.SchedulerConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.AppException;
import mz.org.fgh.hl7.web.SearchForm;
import mz.org.fgh.hl7.web.service.Hl7Service;

@Controller
@RequestMapping("/search")
public class SearchController {

    private Hl7Service hl7Service;
    private SchedulerConfigService config;

    public SearchController(Hl7Service hl7Service, SchedulerConfigService config) {
        this.hl7Service = hl7Service;
        this.config = config;
    }

    @ModelAttribute("searchAvailable")
    public boolean isSearchAvailable() {
        return hl7Service.isSearchAvailable();
    }

    @ModelAttribute("needsNewFile")
    public boolean needsNewFile() {
        return hl7Service.getProcessingResult() == null
                && hl7Service.getHl7File() == null;
    }

    @ModelAttribute("hl7File")
    public HL7File getHl7File() {
        return hl7Service.getHl7File();
    }

    @ModelAttribute("processingResult")
    public CompletableFuture<ProcessingResult> getProcessingResult() {
        return hl7Service.getProcessingResult();
    }

    @ModelAttribute("lastRunTime")
    public LocalDateTime getLastRunTime() {
        return config.getLastRunTime();
    }


    @ModelAttribute("healthFacilities")
    public String getHealthFacilities() {
        HL7File hl7File = hl7Service.getHl7File();
        if (hl7File == null || hl7File.getHealthFacilities() == null) {
            return "";
        }
        return Location.joinLocations(hl7File.getHealthFacilities());
    }

    @ModelAttribute("completedSuccessfully")
    public boolean isCompletedSuccessfully() {
        if (getProcessingResult() != null) {
            return getProcessingResult().isDone() && !getProcessingResult().isCompletedExceptionally();
        }
        return false;
    }

    @ModelAttribute("processedWithErrors")
    public boolean isProcessedWithErrors() throws InterruptedException, ExecutionException {
        if (getProcessingResult() != null && getProcessingResult().isDone()) {
            return !getProcessingResult().get().getErrorLogs().isEmpty();
        }
        return false;
    }

    @GetMapping
    public String search(@Valid SearchForm searchForm,
                         BindingResult bindingResult,
                         Model model,
                         HttpSession session  // Add this parameter
    ) throws FileNotFoundException {

        // Retrieve jobId from session
        String jobId = config.getJobId();
        // Check if jobId exists and add it to the model for the view
        if (jobId != null && !jobId.isEmpty()) {
            model.addAttribute("jobId", jobId);
        }

        try {
            if (bindingResult.hasErrors() || ObjectUtils.isEmpty(searchForm.getPartialNid())) {
                return "search";
            }

            List<PatientDemographic> search = hl7Service.search(searchForm.getPartialNid());

            // Format all birthDates in the list
            for (PatientDemographic patient : search) {
                String originalDate = patient.getBirthDate();
                if (originalDate != null && !originalDate.isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(originalDate); // Assumes format is yyyy-MM-dd
                        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        patient.setBirthDate(formattedDate); // This assumes you have a setter
                    } catch (Exception e) {
                        // Keep original if parsing fails
                        System.out.println("Failed to parse date: " + originalDate);
                    }
                }
            }

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
