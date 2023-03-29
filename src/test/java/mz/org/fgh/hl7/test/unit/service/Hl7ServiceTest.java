package mz.org.fgh.hl7.test.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
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
    public void createShouldGenerateNewHl7File() throws InterruptedException, ExecutionException, HL7Exception {
        String filename = "Test";
        Location location = EASY_RANDOM.nextObject(Location.class);

        PatientDemographic patientDemographics = EASY_RANDOM.nextObject(PatientDemographic.class);

        when(hl7FileGeneratorDao.getPatientDemographicData(anyList()))
                .thenReturn(Arrays.asList(patientDemographics));

        assertThat(hl7Exists(filename)).isFalse();

        hl7Service.create(filename, Arrays.asList(location));

        // TODO remove slee use Futures in create
        Thread.sleep(500);

        assertThat(hl7Exists(filename)).isTrue();
    }

    public boolean hl7Exists(String filename) {
        File file = Paths.get(hl7FolderName, filename + ".hl7").toFile();
        return file.exists() && !file.isDirectory();
    }

}
