package mz.org.fgh.hl7.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;

public interface Hl7Service {
    /**
     * @param locations The locations to be used to generate the HL7 file.
     * @return The generated HL7 file.
     * @throws HL7Exception
     */
    public CompletableFuture<HL7File> generateHl7File(List<Location> locations) throws HL7Exception;

    /**
     * @return The generated HL7 file, could be processing.
     */
    public CompletableFuture<HL7File> getHl7File();

    /**
     * @return Previously HL7 file that was succesfully generated or null if none.
     */
    public HL7File getPreviousHl7File();

    /**
     * @return True if the search is available, false otherwise. Due to asynchronous
     *         nature of the file generation, clients should call this method
     *         before calling search.
     */
    public boolean isSearchAvailable();

    /**
     * @param partialNID The partial NID to be used to search for patients.
     * @return The list of patients matching the partial NID.
     */
    public List<PatientDemographic> search(String partialNID);
}
