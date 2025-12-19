package mz.org.fgh.hl7.web.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.message.ADT_A24;
import ca.uhn.hl7v2.model.v251.segment.PID;
import ca.uhn.hl7v2.model.v251.segment.PV1;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import mz.org.fgh.hl7.lib.service.HL7EncryptionService;
import mz.org.fgh.hl7.web.AppException;
import mz.org.fgh.hl7.web.ProcessingException;
import mz.org.fgh.hl7.web.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.web.generator.AdtMessageFactory;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.HL7FileRequest;
import mz.org.fgh.hl7.web.model.Location;
import mz.org.fgh.hl7.web.model.PatientDemographic;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import mz.org.fgh.hl7.web.util.Hl7Util;

@Service
public class Hl7ServiceImpl implements Hl7Service {

	private static final String METADATA_JSON = ".metadata.json";

	private static final String PROCESSING_PREFIX = ".";

	private static final String HL7_EXTENSION = ".hl7.enc";

	private static final Logger log = LoggerFactory.getLogger(Hl7ServiceImpl.class.getName());

	private HL7EncryptionService encryptionService;

	private Hl7FileGeneratorDao hl7FileGeneratorDao;

	private ObjectMapper objectMapper;

	private CompletableFuture<ProcessingResult> processingResult;

	private ProcessingResult previousProcessingResult;

	private String hl7FolderName;

	private String hl7FileName;

	private String hl7HiddenFileName;

	private String passPhrase;

	public Hl7ServiceImpl(
			HL7EncryptionService encryptionService,
			Hl7FileGeneratorDao hl7FileGeneratorDao,
			ObjectMapper objectMapper,
			@Value("${app.hl7.folder}") String hl7FolderName,
			@Value("${app.hl7.filename}") String fileName,
			@Value("${app.hl7.hidden.filename}") String hiddenFileName,
			@Value("${app.disa.secretKey}") String passPhrase) {

		this.encryptionService = encryptionService;
		this.objectMapper = objectMapper;
		this.hl7FileGeneratorDao = hl7FileGeneratorDao;
		this.hl7FolderName = hl7FolderName;
		this.hl7FileName = fileName;
		this.hl7HiddenFileName = hiddenFileName;
		this.passPhrase = passPhrase;
	}

	@PostConstruct
	public void init() throws IOException, ClassNotFoundException {
		Path path = Paths.get(hl7FolderName);

		// Create HL7 folder if it does not exist
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
				log.info("Created folder {}", hl7FolderName);
			} catch (IOException e) {
				log.error(String.format("Could not create folder %s", hl7FolderName), e);
			}
		}

		// Check if there is a previous processing file
		Path processing = Paths.get(path.toString(), PROCESSING_PREFIX + hl7FileName + HL7_EXTENSION);
		Path done = Paths.get(path.toString(), hl7FileName + HL7_EXTENSION);

		// If there is a previously processed file, delete the temporary file because
		// the previous execution did not finish.
		if (Files.exists(done)) {
			// load serialized HL7File
			HL7File hl7File = new HL7File();
			Path serializePath = Paths.get(hl7FolderName, METADATA_JSON);
			if (Files.exists(serializePath)) {
				hl7File = objectMapper.readValue(Files.newInputStream(serializePath), HL7File.class);
			} else {
				hl7File.setLastModifiedTime(getFileLastModifiedTime());
			}
			ProcessingResult result = new ProcessingResult();
			result.setHl7File(hl7File);
			result.setErrorLogs(Collections.emptyList());
			processingResult = CompletableFuture.completedFuture(result);
			previousProcessingResult = result;
			if (Files.exists(processing)) {
				Files.delete(processing);
			}
		} else if (Files.exists(processing)) {
			Files.delete(processing);
			processingResult = new CompletableFuture<>();
			processingResult.completeExceptionally(new AppException("Previous HL7 file generation did not finish."));
		}
	}

	@Override
	public CompletableFuture<ProcessingResult> getProcessingResult() {
		return processingResult;
	}

	@Override
	public HL7File getHl7File() {
		try {
			if (processingResult != null && processingResult.isDone() && !processingResult.isCompletedExceptionally()) {
				return processingResult.get().getHl7File();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new AppException("hl7.files.processing.error", e);
		}
		return previousProcessingResult != null ? previousProcessingResult.getHl7File() : null;
	}

	@Async
	@Override
	public CompletableFuture<ProcessingResult> generateHl7File(HL7FileRequest hl7FileRequest) {

		if (processingResult != null && !processingResult.isDone()) {
			throw new AppException(
					"Previous HL7 file generation is not yet done. Cancel it if you want to start a new one.");
		}

		Path filePath = Paths.get(hl7FolderName).resolve(hl7FileName + HL7_EXTENSION);

		try {

			processingResult = new CompletableFuture<>();

			List<String> errorLogs = createHl7File(hl7FileRequest, filePath);

			HL7File hl7File = new HL7File();
			hl7File.setProvince(hl7FileRequest.getProvince());
			hl7File.setDistrict(hl7FileRequest.getDistrict());
			hl7File.setHealthFacilities(hl7FileRequest.getHealthFacilities());
			hl7File.setLastModifiedTime(getFileLastModifiedTime());

			// Serialize the HL7File object
			Path serializePath = Paths.get(hl7FolderName, METADATA_JSON);
			Files.deleteIfExists(serializePath);
			objectMapper.writeValue(new File(serializePath.toString()), hl7File);

			ProcessingResult result = new ProcessingResult();
			result.setHl7File(hl7File);
			result.setErrorLogs(errorLogs);
			processingResult.complete(result);

			// Set this as the previous successfuly generated HL7 file
			previousProcessingResult = result;

		} catch (IOException | RuntimeException e) {

			log.error("Error creating hl7", e);

			if (processingResult == null) {
				processingResult = new CompletableFuture<>();
			}
			processingResult.completeExceptionally(e);

		}

		return processingResult;
	}

	public boolean isSearchAvailable() {
		if (previousProcessingResult != null) {
			return true;
		} else {
			return processingResult != null && !processingResult.isCompletedExceptionally();
		}
	}

	public List<PatientDemographic> search(String partialNID) {
		File selectedFile = new File(Paths.get(hl7FolderName, hl7HiddenFileName + HL7_EXTENSION).toString());

		if (!selectedFile.exists()) {
			throw new AppException("hl7.search.error.file.not.found");
		}

		try (InputStream inputStream = encryptionService.decrypt(selectedFile.toPath(), passPhrase)) {

			Hl7InputStreamMessageIterator iter = new Hl7InputStreamMessageIterator(inputStream);

			List<PatientDemographic> demographicData = new ArrayList<>();

			while (iter.hasNext()) {

				Message hapiMsg = iter.next();

				ADT_A24 adtMsg = (ADT_A24) hapiMsg;
				PID pid = adtMsg.getPID();
				PV1 pv1 = adtMsg.getPV1();

				PatientDemographic data = new PatientDemographic();

				data.setPid(pid.getPatientID().getIDNumber().getValue().trim());

				data.setGivenName(fixEncoding(pid.getPatientName(0).getGivenName().getValue()));
				data.setMiddleName(fixEncoding(pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue()));
				data.setFamilyName(fixEncoding(pid.getPatientName(0).getFamilyName().getSurname().getValue()));

				setLastConsultationDate(pv1, data);
				data.setBirthDate(pid.getDateTimeOfBirth().getTime().getValue());
				data.setGender(pid.getAdministrativeSex().getValue());

				data.setAddress(fixEncoding(pid.getPatientAddress(0).getStreetAddress().getStreetName().getValue()));
				data.setCountyDistrict(fixEncoding(pid.getPatientAddress(0).getCity().getValue()));
				data.setStateProvince(fixEncoding(pid.getPatientAddress(0).getStateOrProvince().getValue()));
				setLocationName(adtMsg, data);

				demographicData.add(data);
			}

			List<PatientDemographic> filteredDemo = new ArrayList<>();

			for (PatientDemographic data : demographicData) {
				if (data.getPid().contains(partialNID)) {
					filteredDemo.add(data);
				}
			}

			return filteredDemo;

		} catch (IOException e) {
			throw new AppException("hl7.search.error", e);
		}
	}

	private String fixEncoding(String input) {
		if (input == null) return null;
		try {
			return new String(input.getBytes("ISO-8859-1"), "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return input;
		}
	}

	private void setLastConsultationDate(PV1 pv1, PatientDemographic data) {
		try {
			Date date = pv1.getAdmitDateTime().getTime().getValueAsDate();
			if (date != null) {
				data.setLastConsultationDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			}
		} catch (DataTypeException e) {
			log.debug("Could not set last consultation date", e);
		}
	}

	private void setLocationName(ADT_A24 adtMsg, PatientDemographic data) {
		try {
			data.setLocationName(adtMsg.getMSH().getSendingFacility().encode());
		} catch (HL7Exception e) {
			log.debug("Could not encode sendingFacility", e);
		}
	}

	private List<String> createHl7File(HL7FileRequest hl7FileRequest, Path filePath) throws IOException {
		log.info("createHl7File called...");

		List<String> locationsByUuid = hl7FileRequest.getHealthFacilities().stream().map(Location::getUuid)
				.collect(Collectors.toList());

		String currentTimeStamp = Hl7Util.getCurrentTimeStamp();

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

		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

			log.info("Serializing message...");

			byteArrayOutputStream.write(headers.getBytes());

			// we encrypt patient at a time
			List<String> errorLogs = processDemographics(patientDemographics, pipeParser, byteArrayOutputStream);

			String footers = "BTS|" + String.valueOf(patientDemographics.size()) + "\rFTS|1";

			byteArrayOutputStream.write(footers.getBytes());

			encryptionService.encrypt(byteArrayOutputStream, passPhrase, filePath);

			// Create a copy of the encrypted file with a hidden filename
			Path destinationPath = filePath.resolveSibling(".Hidden." + filePath.getFileName().toString());
			Files.copy(filePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
			Files.setAttribute(destinationPath, "dos:hidden", true);

			log.info("Message serialized to file {} successfully", filePath);

			return errorLogs;
		}
	}

	private List<String> processDemographics(List<PatientDemographic> patientDemographics, PipeParser pipeParser,
			ByteArrayOutputStream byteArrayOutputStream) throws IOException {
		List<String> errorLogs = new ArrayList<>();
		for (PatientDemographic patient : patientDemographics) {
			try {
				ADT_A24 adtMessage = AdtMessageFactory.createMessage("A24", patient);
				byteArrayOutputStream.write(pipeParser.encode(adtMessage).getBytes());
				byteArrayOutputStream.write(System.getProperty("line.separator").getBytes());
				byteArrayOutputStream.flush();
			} catch (HL7Exception e) {
				String msg = patient.getPid()
						+ ": Erro de processamento inesperado, deve contactar o administrador de sistemas.";
				errorLogs.add(msg);
				log.warn(msg, e);
			} catch (ProcessingException e) {
				errorLogs.add(e.getMessage());
				e.printStackTrace();
			}
		}
		return errorLogs;
	}

	public LocalDateTime getFileLastModifiedTime() {
		try {
			Path path = Paths.get(hl7FolderName, hl7FileName + HL7_EXTENSION);
			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
			return attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

		} catch (NoSuchFileException e) {
			log.warn("No existing HL7 file found (fresh install). Returning null for lastModifiedTime.");
			return null;
		} catch (IOException e) {
			log.error("Failed to read HL7 file attributes: {}", e.getMessage(), e);
			throw new AppException("hl7.create.error", e);
		}
	}
}
