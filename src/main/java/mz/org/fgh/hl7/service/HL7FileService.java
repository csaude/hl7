package mz.org.fgh.hl7.service;

import java.util.List;

import mz.org.fgh.hl7.HL7File;

public interface HL7FileService {
    public void create(String filename);
    public byte[] read(String filename);
    public void delete(String filename);
    public List<HL7File> findAll();
}
