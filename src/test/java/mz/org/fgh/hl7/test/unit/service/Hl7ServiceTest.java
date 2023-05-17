package mz.org.fgh.hl7.test.unit.service;

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
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.service.Hl7Service;

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
        hl7FilePath = Paths.get(hl7FolderName, hl7FileName + ".hl7");
    }

    @AfterEach
    public void afterEach() {
        // Delete all .hl7 files
        File hl7Folder = Paths.get(hl7FolderName).toFile();
        for (File f : hl7Folder.listFiles()) {
            if (f.getName().endsWith(".hl7")) {
                f.delete();
            }
        }
    }

    @Test
    public void createShouldGenerateNewHl7File()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = EASY_RANDOM.nextObject(Location.class);

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));

        assertThat(hl7Exists(hl7FilePath)).isFalse();

        CompletableFuture<HL7File> hl7File = hl7Service.generateHl7File(Arrays.asList(location));

        LocalDateTime lastModifiedTime = hl7File.get().getLastModifiedTime();

        assertThat(hl7Exists(hl7FilePath)).isTrue();
        assertThat(lastModifiedTime).isNotNull();
    }

    @Test
    public void createShouldRemoveTemporaryHl7File()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = EASY_RANDOM.nextObject(Location.class);

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));

        CompletableFuture<HL7File> hl7File = hl7Service.generateHl7File(Arrays.asList(location));

        hl7File.get();

        assertThat(hl7Exists(hl7FilePath.resolveSibling("." + hl7FileName))).isFalse();
    }

    @Test
    public void createShouldSavePreviousLastModifiedTime()
            throws InterruptedException, ExecutionException, HL7Exception, TimeoutException {
        Location location = EASY_RANDOM.nextObject(Location.class);

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));
        CompletableFuture<HL7File> hl7File = hl7Service.generateHl7File(Arrays.asList(location));
        LocalDateTime time1 = hl7File.get().getLastModifiedTime();
        assertThat(hl7Service.getPreviousHl7File().getLastModifiedTime()).isEqualTo(time1);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenThrow(new RuntimeException());
        CompletableFuture<HL7File> hl7File2 = hl7Service.generateHl7File(Arrays.asList(location));
        try {
            hl7File2.join();
        } catch (RuntimeException e) {
        }

        assertThat(hl7Service.getPreviousHl7File().getLastModifiedTime()).isEqualTo(time1);
    }

    public boolean hl7Exists(Path hl7Path) {
        File file = hl7Path.toFile();
        return file.exists() && !file.isDirectory();
    }

}
