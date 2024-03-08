package mz.org.fgh.hl7.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.generator.AdtMessageFactory;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.HL7FileRequest;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.util.Hl7Util;

@Service
public class Hl7ServiceImpl implements Hl7Service {

	@Autowired
	private HL7EncryptionService encryptionService;

	private static final String METADATA_JSON = ".metadata.json";

	private static final String PROCESSING_PREFIX = ".";

	private static final String HL7_EXTENSION = ".hl7.enc";

	private static final String BAK_EXTENSION = ".bak";
	
	//TODO should be collected from somewhere safe
	private static final String PASSPHRASE = "D1s@l1nKF1l3Encrypt10n";

	private static final Logger log = LoggerFactory.getLogger(Hl7ServiceImpl.class.getName());

	private Hl7FileGeneratorDao hl7FileGeneratorDao;

	private ObjectMapper objectMapper;

	private CompletableFuture<HL7File> hl7FileFuture;

	private HL7File previousHl7File;

	private String hl7FolderName;

	private String hl7FileName;

	public Hl7ServiceImpl(Hl7FileGeneratorDao hl7FileGeneratorDao, ObjectMapper objectMapper,
			@Value("${app.hl7.folder}") String hl7FolderName, @Value("${app.hl7.filename}") String fileName) {
		this.objectMapper = objectMapper;
		this.hl7FileGeneratorDao = hl7FileGeneratorDao;
		this.hl7FolderName = hl7FolderName;
		this.hl7FileName = fileName;
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
			hl7FileFuture = CompletableFuture.completedFuture(hl7File);
			previousHl7File = hl7File;
			if (Files.exists(processing))
				Files.delete(processing);
		} else if (Files.exists(processing)) {
			Files.delete(processing);
			hl7FileFuture = new CompletableFuture<>();
			hl7FileFuture.completeExceptionally(new AppException("Previous HL7 file generation did not finish."));
		}
	}

	@Override
	public CompletableFuture<HL7File> getHl7FileFuture() {
		return hl7FileFuture;
	}

	@Override
	public HL7File getHl7File() {
		try {
			if (hl7FileFuture != null && hl7FileFuture.isDone() && !hl7FileFuture.isCompletedExceptionally()) {
				return hl7FileFuture.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new AppException("hl7.files.processing.error", e);
		}
		return previousHl7File;
	}

	@Async
	@Override
	public CompletableFuture<HL7File> generateHl7File(HL7FileRequest hl7FileRequest) throws HL7Exception {

		if (hl7FileFuture != null && !hl7FileFuture.isDone()) {
			throw new AppException(
					"Previous HL7 file generation is not yet done. Cancel it if you want to start a new one.");
		}

		Path processing = Paths.get(hl7FolderName).resolve(PROCESSING_PREFIX + hl7FileName + HL7_EXTENSION);

		try {

			hl7FileFuture = new CompletableFuture<>();

			createHl7File(hl7FileRequest, processing);

			HL7File hl7File = new HL7File();
			hl7File.setProvince(hl7FileRequest.getProvince());
			hl7File.setDistrict(hl7FileRequest.getDistrict());
			hl7File.setHealthFacilities(hl7FileRequest.getHealthFacilities());
			hl7File.setLastModifiedTime(getFileLastModifiedTime());

			// Serialize the HL7File object
			Path serializePath = Paths.get(hl7FolderName, METADATA_JSON);
			Files.deleteIfExists(serializePath);
			objectMapper.writeValue(new File(serializePath.toString()), hl7File);

			hl7FileFuture.complete(hl7File);

			// Set this as the previous successfuly generated HL7 file
			previousHl7File = hl7File;

		} catch (IOException | HL7Exception | RuntimeException | 
				InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException |
				InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {

			log.error("Error creating hl7", e);

			if (hl7FileFuture == null) {
				hl7FileFuture = new CompletableFuture<>();
			}
			hl7FileFuture.completeExceptionally(e);

		} finally {
			if (Files.exists(processing)) {
				try {
					Files.delete(processing);
				} catch (IOException e) {
					log.error("Error deleting processing file", e);
				}
			}
		}

		return hl7FileFuture;
	}

	public boolean isSearchAvailable() {
		if (previousHl7File != null) {
			return true;
		} else {
			return hl7FileFuture != null && !hl7FileFuture.isCompletedExceptionally();
		}
	}

	public List<PatientDemographic> search(String partialNID) {

		if (!hl7FileFuture.isDone()) {
			throw new AppException("hl7.search.error.not.done");
		}

		File hlfF = new File(Paths.get(hl7FolderName, hl7FileName + HL7_EXTENSION).toString());

		File bakF = new File(Paths.get(hl7FolderName, hl7FileName + BAK_EXTENSION).toString());

		File selectedFile = hlfF.exists() ? hlfF : bakF;

		if (!selectedFile.exists()) {
			throw new AppException("hl7.search.error.file.not.found");
		}

		try (InputStream inputStream = encryptionService.desincrypt(selectedFile.toPath(), PASSPHRASE)) {

			Hl7InputStreamMessageIterator iter = new Hl7InputStreamMessageIterator(inputStream);

			List<PatientDemographic> demographicData = new ArrayList<>();

			while (iter.hasNext()) {

				Message hapiMsg = iter.next();

				ADT_A24 adtMsg = (ADT_A24) hapiMsg;
				PID pid = adtMsg.getPID();
				PV1 pv1 = adtMsg.getPV1();

				PatientDemographic data = new PatientDemographic();

				data.setPid(pid.getPatientID().getIDNumber().getValue().trim());
				data.setGivenName(pid.getPatientName(0).getGivenName().getValue());
				data.setMiddleName(pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
				data.setFamilyName(pid.getPatientName(0).getFamilyName().getSurname().getValue());
				setLastConsultationDate(pv1, data);
				data.setBirthDate(pid.getDateTimeOfBirth().getTime().getValue());
				data.setGender(pid.getAdministrativeSex().getValue());
				data.setAddress(pid.getPatientAddress(0).getStreetAddress().getStreetName().getValue());
				data.setCountyDistrict(pid.getPatientAddress(0).getCity().getValue());
				data.setStateProvince(pid.getPatientAddress(0).getStateOrProvince().getValue());
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

	private void createHl7File(HL7FileRequest hl7FileRequest, Path filePath)
			throws IOException, HL7Exception, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
		log.info("createHl7File called...");

		Files.createFile(filePath);

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

		try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

			log.info("Serializing message...");

			byteArrayOutputStream.write(headers.getBytes()); 
			
			// we encrypt patient at a time
			for (PatientDemographic patient : patientDemographics) {
				ADT_A24 adtMessage = AdtMessageFactory.createMessage("A24", patient);
				byteArrayOutputStream.write(pipeParser.encode(adtMessage).getBytes());
				byteArrayOutputStream.write(System.getProperty("line.separator").getBytes());
				byteArrayOutputStream.flush();
			}

			String footers = "BTS|" + String.valueOf(patientDemographics.size()) + "\rFTS|1";

			byteArrayOutputStream.write(footers.getBytes());

			// Remove the dot from file name to mark as done processing
			String processingfileName = filePath.getFileName().toString();
			String doneFileName = processingfileName.split("\\.")[1];
			Path donePath = filePath.resolveSibling(doneFileName + HL7_EXTENSION);
			
			encryptionService.encrypt(byteArrayOutputStream, PASSPHRASE, donePath); 

			log.info("Message serialized to file {} successfully", donePath);
		}
	}

	private LocalDateTime getFileLastModifiedTime() {
		try {
			Path path = Paths.get(hl7FolderName, hl7FileName + HL7_EXTENSION);
			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
			return attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		} catch (IOException e) {
			throw new AppException("hl7.create.error", e);
		}
	}
}
