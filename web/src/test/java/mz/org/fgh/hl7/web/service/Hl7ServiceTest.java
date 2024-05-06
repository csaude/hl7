package mz.org.fgh.hl7.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.web.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.web.model.HL7FileRequest;
import mz.org.fgh.hl7.web.model.Location;
import mz.org.fgh.hl7.web.model.PatientDemographic;
import mz.org.fgh.hl7.web.model.ProcessingResult;

@SpringBootTest
@AutoConfigureMockMvc
public class Hl7ServiceTest {

    private static EasyRandom EASY_RANDOM = new EasyRandom();

    @Autowired
    Hl7Service hl7Service;

    @MockBean
    Hl7FileGeneratorDao hl7FileGeneratorDao;

    @Value("${app.hl7.folder}")
    String hl7FolderName;

    @Value("${app.hl7.filename}")
    private String hl7FileName;

    private Path hl7FilePath;

    @BeforeEach
    public void beforeEach() {
        hl7FilePath = Paths.get(hl7FolderName, hl7FileName + ".hl7.enc");
    }

    @AfterEach
    public void afterEach() {
        // Delete all .hl7 files
        File hl7Folder = Paths.get(hl7FolderName).toFile();
        for (File f : hl7Folder.listFiles()) {
            if (f.getName().endsWith(".hl7.enc")) {
                f.delete();
            }
        }
    }

    @Test
    public void createShouldGenerateNewHl7File()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = new Location();

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));

        assertThat(hl7Exists(hl7FilePath)).isFalse();

        HL7FileRequest hl7FileRequest = new HL7FileRequest();
        hl7FileRequest.setHealthFacilities(Arrays.asList(location));
        CompletableFuture<ProcessingResult> hl7File = hl7Service.generateHl7File(hl7FileRequest);

        LocalDateTime lastModifiedTime = hl7File.get().getHl7File().getLastModifiedTime();

        assertThat(hl7Exists(hl7FilePath)).isTrue();
        assertThat(lastModifiedTime).isNotNull();
    }

    @Test
    public void createShouldRemoveTemporaryHl7File()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = new Location();

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));

        HL7FileRequest hl7FileRequest = new HL7FileRequest();
        hl7FileRequest.setHealthFacilities(Arrays.asList(location));
        CompletableFuture<ProcessingResult> hl7File = hl7Service.generateHl7File(hl7FileRequest);

        hl7File.get();

        assertThat(hl7Exists(hl7FilePath.resolveSibling("." + hl7FileName))).isFalse();
    }

    @Test
    public void createShouldSavePreviousLastModifiedTime()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = new Location();

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        HL7FileRequest hl7FileRequest = new HL7FileRequest();
        hl7FileRequest.setHealthFacilities(Arrays.asList(location));

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));
        CompletableFuture<ProcessingResult> hl7File = hl7Service.generateHl7File(hl7FileRequest);
        LocalDateTime time1 = hl7File.get().getHl7File().getLastModifiedTime();
        assertThat(hl7Service.getHl7File().getLastModifiedTime()).isEqualTo(time1);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenThrow(new RuntimeException());

        CompletableFuture<ProcessingResult> hl7File2 = hl7Service.generateHl7File(hl7FileRequest);
        try {
            hl7File2.join();
        } catch (RuntimeException e) {
        }

        assertThat(hl7Service.getHl7File().getLastModifiedTime()).isEqualTo(time1);
    }

    public boolean hl7Exists(Path hl7Path) {
        File file = hl7Path.toFile();
        return file.exists() && !file.isDirectory();
    }

}
