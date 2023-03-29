package mz.org.fgh.hl7.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.message.ADT_A24;
import ca.uhn.hl7v2.parser.PipeParser;
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.generator.AdtMessageFactory;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.HL7File.ProcessingStatus;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.util.Util;

@Service
public class Hl7ServiceImpl implements Hl7Service {

	static final Logger log = LoggerFactory.getLogger(Hl7ServiceImpl.class.getName());

	private static final String PROCESSING_PREFIX = ".";

	private static String HL7_EXTENSION = ".hl7";

	private final ConcurrentHashMap<String, ProcessingStatus> processingStatus = new ConcurrentHashMap<>();

	private Hl7FileGeneratorDao hl7FileGeneratorDao;

	private String hl7FolderName;

	public Hl7ServiceImpl(Hl7FileGeneratorDao hl7FileGeneratorDao, @Value("${app.hl7.folder}") String hl7FolderName) {
		this.hl7FileGeneratorDao = hl7FileGeneratorDao;
		this.hl7FolderName = hl7FolderName;
	}

	@PostConstruct
	public void initProcessingStatusMap() throws IOException {
		Path path = Paths.get(hl7FolderName);
		try (Stream<Path> paths = Files.list(path)) {
			paths
				.filter(p -> p.toString().endsWith(HL7_EXTENSION))
				.forEach(p -> {
					String filename = p.getFileName().toString();
					// Get the processing status map key, which should be the
					// filename without PROCESSING_PREFIX.
					String key = filename;
					if (key.startsWith(PROCESSING_PREFIX)) {
						key = key.substring(1);
					}

					// If prefix is present it means that it either failed, or it is processing.
					// We'll consider the processing as failed because if it is not yet in the
					// processing status map, the app probably crashed.
					if (filename.startsWith(PROCESSING_PREFIX)) {
						processingStatus.put(key, ProcessingStatus.FAILED);
					} else {
						processingStatus.put(key, ProcessingStatus.DONE);
					}
				});
		}
	}

	public List<HL7File> findAll() {
		try {
			Path path = Paths.get(hl7FolderName);
			try (Stream<Path> paths = Files.list(path)) {
				return paths
						.filter(p -> p.toString().endsWith(HL7_EXTENSION))
						.map(this::buildHL7File)
						.sorted((a, b) -> b.getLastModifiedTime().compareTo(a.getLastModifiedTime()))
						.collect(Collectors.toList());
			}
		} catch (IOException e) {
			throw new AppException("While listing hl7 file directory", e);
		}
	}

	public byte[] read(String filename) {

		Path path = Paths.get(hl7FolderName).resolve(filename);

		// Should not read file being processed
		if (Files.exists(path.resolveSibling(PROCESSING_PREFIX + filename))) {
			throw new AppException("hl7.read.error.processing");
		}

		// Should not read non hl7 files
		HL7File hl7 = buildHL7File(path);
		int dotIndex = hl7.getFileName().lastIndexOf(".");
		if (dotIndex > 0 && !hl7.getFileName().substring(dotIndex).equals(HL7_EXTENSION)) {
			throw new AppException("hl7.read.error.notHL7");
		}

		// Should not read files outside defined hl7 folder
		checkIfInHL7Folder(path);

		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new AppException("hl7.read.error", e);
		}
	}

	public void validateCreate(String filename) {

		Path processing = Paths.get(hl7FolderName)
				.resolve(PROCESSING_PREFIX + filename + HL7_EXTENSION);

		Path processed = processing.resolveSibling(filename + HL7_EXTENSION);

		// Should not create existing file
		String key = processed.getFileName().toString();
		if (processingStatus.containsKey(key)) {
			throw new AppException("hl7.create.error.exists");
		}

		// Should not create files outside defined hl7 folder
		checkIfInHL7Folder(processing);
	}

	public void delete(String filename) {

		Path path = Paths.get(hl7FolderName).resolve(filename);

		// Should not delete processing file
		if (processingStatus.get(filename) == ProcessingStatus.PROCESSING) {
			throw new AppException("hl7.delete.error.processing");
		}

		// Should not delete file outside defined hl7 folder
		checkIfInHL7Folder(path);

		try {

			Path deletePath = path;
			// If it failed it should still have the PROCESSING_PREFIX, so
			// we need to make sure to delete the correct filename.
			if (processingStatus.get(filename) == ProcessingStatus.FAILED) {
				deletePath = path.resolveSibling(PROCESSING_PREFIX + path.getFileName().toString());
			}

			Files.delete(deletePath);

			processingStatus.remove(filename);

		} catch (IOException e) {
			throw new AppException("hl7.delete.error", e);
		}
	}

	@Async
	public void create(String fileName, List<Location> locations) throws HL7Exception {

		validateCreate(fileName);

		Path processing = Paths.get(hl7FolderName)
				.resolve(PROCESSING_PREFIX + fileName + HL7_EXTENSION);

		Path processed = processing.resolveSibling(fileName + HL7_EXTENSION);

		String key = processed.getFileName().toString();

		try {

			log.info("createHl7File called...");

			// File should be created as early as possible so that it
			// is possible to see it processing.
			Files.createFile(processing);

			processingStatus.put(key, ProcessingStatus.PROCESSING);

			List<String> locationsByUuid = locations.stream()
					.map(Location::getUuid)
					.collect(Collectors.toList());

			String currentTimeStamp = Util.getCurrentTimeStamp();

			// prepare the headers
			String headers = "FHS|^~\\&|XYZSYS|XYZ " + "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp
					+ "||Patient_Demographic_Data.hl7|" + "WEEKLY HL7 UPLOAD|00009972|\rBHS|^~\\&|XYZSYS|XYZ "
					+ "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp + "||||00010223\r";

			// create and write the HL7 message to file
			log.info("Fetching patient demographics.");
			List<PatientDemographic> patientDemographics = hl7FileGeneratorDao.getPatientDemographicData(locationsByUuid);
			PipeParser pipeParser = new PipeParser();
			pipeParser.getParserConfiguration();
			log.info("Done fetching patient demographics.");

			// serialize the message to pipe delimited output file
			try (OutputStream outputStream = Files.newOutputStream(processing)) {

				log.info("Serializing message to file...");

				outputStream.write(headers.getBytes());

				for (PatientDemographic patient : patientDemographics) {
					ADT_A24 adtMessage = AdtMessageFactory.createMessage("A24", patient);
					outputStream.write(pipeParser.encode(adtMessage).getBytes());
					outputStream.write(System.getProperty("line.separator").getBytes());
					outputStream.flush();
				}

				String footers = "BTS|" + String.valueOf(patientDemographics.size()) + "\rFTS|1";

				outputStream.write(footers.getBytes());

				// Remove the dot from file name to mark as done processing
				String processingfileName = processing.getFileName().toString();
				String doneFileName = processingfileName.split("\\.")[1];
				Path donePath = processing.resolveSibling(doneFileName + HL7_EXTENSION);
				Files.move(processing, donePath);

				log.info("Message serialized to file {} successfully", donePath);
			}

			processingStatus.put(key, ProcessingStatus.DONE);

		} catch (Exception e) {

			processingStatus.put(key, ProcessingStatus.FAILED);

			log.error("Error creating hl7", e);

			throw new AppException("hl7.create.error", e);

		}

	}

	private HL7File buildHL7File(Path path) {
		try {

			HL7File hl7 = new HL7File();

			String filename = path.getFileName().toString();

			// If processing remove '.' from start of filename
			if (filename.startsWith(PROCESSING_PREFIX)) {
				hl7.setFileName(filename.substring(1));
			} else {
				hl7.setFileName(filename);
			}

			// Get the processing status map key, which should be the
			// filename without PROCESSING_PREFIX.
			String key = filename;
			if (key.startsWith(PROCESSING_PREFIX)) {
				key = key.substring(1);
			}
			hl7.setProcessingStatus(processingStatus.get(key));

			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
			LocalDateTime lastModified = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			hl7.setLastModifiedTime(lastModified);
			return hl7;
		} catch (IOException e) {
			throw new AppException("hl7.create.error", e);
		}
	}

	private void checkIfInHL7Folder(Path path) {
		if (!path.normalize().startsWith(Paths.get(hl7FolderName))) {
			throw new AppException("hl7.error.folder");
		}
	}
}
