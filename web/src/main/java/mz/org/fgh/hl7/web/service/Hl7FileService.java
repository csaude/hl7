package mz.org.fgh.hl7.web.service;

import mz.org.fgh.hl7.web.model.HL7File;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
public interface Hl7FileService {
    /**
     * Get HL7 file information
     * @return HL7File object with location information
     */
    HL7File getHl7File();

    /**
     * Get list of generated HL7 files for a location
     * @param locationUUID the UUID of the location
     * @return List of file information maps
     */
    List<Map<String, String>> getGeneratedFiles(String locationUUID);

    /**
     * Download and save an HL7 file from a URL
     * @param fileUrl the URL to download the file from
     * @throws IOException if there's an error downloading or saving the file
     */
    void downloadAndSaveFile(String fileUrl) throws IOException;

    /**
     * Get the job ID from the configuration
     * @return the job ID
     */
//    String getConfigJobId();

    /**
     * Check the status of a job
     * @param jobId the ID of the job to check
     * @return a map containing status information
     * @throws Exception if there's an error checking the status
     */
    ResponseEntity<Map<String, Object>> checkJobStatus() throws Exception;
}
