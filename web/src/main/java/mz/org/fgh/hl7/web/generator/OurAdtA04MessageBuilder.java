package mz.org.fgh.hl7.web.generator;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v251.datatype.XAD;
import ca.uhn.hl7v2.model.v251.datatype.XPN;
import ca.uhn.hl7v2.model.v251.message.ADT_A24;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.model.v251.segment.PID;
import ca.uhn.hl7v2.model.v251.segment.PV1;
import mz.org.fgh.hl7.web.ProcessingException;
import mz.org.fgh.hl7.web.model.PatientDemographic;
import mz.org.fgh.hl7.web.util.Hl7Util;

/**
 * @author machabane
 */
public class OurAdtA04MessageBuilder {

	static final Logger LOG = Logger.getLogger(OurAdtA04MessageBuilder.class.getName());

	private ADT_A24 _adtMessage;

	public ADT_A24 Build(PatientDemographic demographic) throws HL7Exception, IOException {
		String currentDateTimeString = Hl7Util.getCurrentTimeStamp();
		_adtMessage = new ADT_A24();
		_adtMessage.initQuickstart("ADT", "A24", "P");
		createMshSegment(currentDateTimeString, demographic);
		createPidSegment(demographic);
		createPv1Segment(demographic);
		return _adtMessage;
	}

	private void createMshSegment(String currentDateTimeString, PatientDemographic demographic)
			throws DataTypeException {
		MSH mshSegment = _adtMessage.getMSH();
		mshSegment.getFieldSeparator().setValue("|");
		mshSegment.getEncodingCharacters().setValue("^~\\&");
		mshSegment.getSendingApplication().getNamespaceID().setValue("XYZSYS");
		mshSegment.getSendingFacility().getNamespaceID()
				.setValue(demographic.getLocationName());
		mshSegment.getReceivingApplication().getNamespaceID().setValue("DISA*LAB");
		mshSegment.getReceivingFacility().getNamespaceID().setValue("***");
		mshSegment.getDateTimeOfMessage().getTime().setValue(currentDateTimeString);
		mshSegment.getMessageControlID().setValue(getSequenceNumber());
		mshSegment.getVersionID().getVersionID().setValue("2.5.1");
	}

	private void createPidSegment(PatientDemographic demographic) throws DataTypeException {
		PID pid = _adtMessage.getPID();
		XPN patientName = pid.getPatientName(0);
		patientName.getFamilyName().getFn1_Surname().setValue(demographic.getFamilyName());
		patientName.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(demographic.getMiddleName());
		patientName.getGivenName().setValue(demographic.getGivenName());
		pid.getDateTimeOfBirth().getTime().setValue(demographic.getBirthDate());
		pid.getAdministrativeSex().setValue(demographic.getGender());
		pid.getPatientID().getIDNumber().setValue(demographic.getPid());
		pid.getMaritalStatus().getText().setValue(demographic.getMaritalStatus());
		XAD patientAddress = pid.getPatientAddress(0);
		patientAddress.getStreetAddress().getStreetName().setValue(demographic.getAddress());
		patientAddress.getCity().setValue(demographic.getCountyDistrict());
		patientAddress.getStateOrProvince().setValue(demographic.getStateProvince());
		patientAddress.getCountry().setValue(demographic.getCountry());
	}

	private void createPv1Segment(PatientDemographic demographic) throws DataTypeException {
		PV1 pv1 = _adtMessage.getPV1();
		pv1.getSetIDPV1().setValue("1");
		pv1.getPatientClass().setValue("O");
		pv1.getAssignedPatientLocation().getPl9_LocationDescription()
				.setValue("Default Location");
		pv1.getAdmissionType().setValue("R");
		if (demographic.getLastConsultationDate() != null) {
			Instant instant = demographic.getLastConsultationDate().atZone(ZoneId.systemDefault()).toInstant();
			try {
				pv1.getAdmitDateTime().getTime().setValue(Date.from(instant));
			} catch (DataTypeException e) {
				LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
				throw new ProcessingException(
						demographic.getPid() + ": Data da última consulta (" + localDate + ") é inválida.", e);
			}
		}
	}

	private String getSequenceNumber() {
		String facilityNumberPrefix = "Location ID";
		return facilityNumberPrefix.concat(Hl7Util.getCurrentTimeStamp());
	}
}
