package mz.org.fgh.hl7.web.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.message.ADT_A24;
import ca.uhn.hl7v2.model.v251.segment.PID;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.web.SearchForm;

@Controller
@RequestMapping("hl7/{filename}")
public class SearchController {

    @Value("${app.hl7.folder}")
    private String hl7LocationFolder;

    @GetMapping("/search")
    public String search(@Valid SearchForm searchForm,
            BindingResult bindingResult,
            @PathVariable("filename") String filename,
            Model model) throws FileNotFoundException {


        if (bindingResult.hasErrors() || StringUtils.isEmpty(searchForm.getPartialNid())) {
            return "search";
        }

        File hlfF = new File(
                Paths.get(hl7LocationFolder,  filename)
                        .toString());

        InputStream inputStream = new BufferedInputStream(new FileInputStream(hlfF));

        Hl7InputStreamMessageIterator iter = new Hl7InputStreamMessageIterator(inputStream);

        List<PatientDemographic> demographicData = new ArrayList<>();

        while (iter.hasNext()) {

            Message hapiMsg = iter.next();

            ADT_A24 adtMsg = (ADT_A24) hapiMsg;
            PID pid = adtMsg.getPID();

            PatientDemographic data = new PatientDemographic();

            data.setPid(pid.getPatientID().getIDNumber().getValue().trim());
            data.setGivenName(pid.getPatientName(0).getGivenName().getValue());
            data.setMiddleName(pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
            data.setFamilyName(pid.getPatientName(0).getFamilyName().getSurname().getValue());
            data.setBirthDate(pid.getDateTimeOfBirth().getTime().getValue());
            data.setGender(pid.getAdministrativeSex().getValue());
            data.setAddress(pid.getPatientAddress(0).getStreetAddress().getStreetName().getValue());
            data.setCountryDistrict(pid.getPatientAddress(0).getCity().getValue());
            data.setStateProvince(pid.getPatientAddress(0).getStateOrProvince().getValue());

            demographicData.add(data);
        }

        List<PatientDemographic> filteredDemo = new ArrayList<>();

        for (PatientDemographic data : demographicData) {
            if (data.getPid().contains(searchForm.getPartialNid())) {
                filteredDemo.add(data);
            }
        }

        if (filteredDemo.isEmpty()) {
            model.addAttribute("errorMessage",
                    "Não encontramos nenhum item que corresponda à sua consulta de pesquisa.");
        } else {

            model.addAttribute("hl7Patients", filteredDemo);
        }

        return "search";

    }
}