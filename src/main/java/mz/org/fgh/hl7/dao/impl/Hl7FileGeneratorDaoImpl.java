package mz.org.fgh.hl7.dao.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.util.Util;

@Repository
public class Hl7FileGeneratorDaoImpl implements Hl7FileGeneratorDao {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private String sql;
	
	@Override
	public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid) {
		
		sql = "select REPLACE(REPLACE(pid.identifier, '\r', ''), '\n', ' ') pid,"
		        + "		pe.gender,"
		        + "		pe.birthdate,"
		        + "		REPLACE(REPLACE(pn.given_name, '\r', ''), '\n', ' ') given_name,"
		        + "		REPLACE(REPLACE(pn.middle_name, '\r', ''), '\n', ' ') middle_name,"
		        + "		REPLACE(REPLACE(pn.family_name, '\r', ''), '\n', ' ') family_name,"
		        + "		REPLACE(REPLACE(CONCAT(TRIM(IFNULL(pa.address1, '')),' ',TRIM(IFNULL(pa.address2, '')),' ',TRIM(IFNULL(pa.address3, '')),' ',TRIM(IFNULL(pa.address6, '')),' ',TRIM(IFNULL(pa.address5, ''))), '\r', ''), '\n', ' ') address,"
		        + "		REPLACE(REPLACE(pa.state_province, '\r', ''), '\n', ' ') state_province,"
		        + "		REPLACE(REPLACE(pa.country, '\r', ''), '\n', ' ') country,"
		        + "		REPLACE(REPLACE(pa.county_district, '\r', ''), '\n', ' ') county_district,"
		        + "		REPLACE(REPLACE(pat.value, '\r', ''), '\n', ' ') telefone1,"
		        + "		REPLACE(REPLACE(pat1.value, '\r', ''), '\n', ' ') telefone2," + "		CASE pat2.value"
		        + "   			WHEN 1057 THEN 'S'" + "   			WHEN 5555 THEN 'M'" + "   			WHEN 1060 THEN 'P'"
		        + "   			WHEN 1059 THEN 'W'" + "   			WHEN 1056 THEN 'D'" + "   		ELSE 'T'" + "		END marital_status,"
		        + "		pid.location_id" + " from" + " person pe " + "inner join patient p on pe.person_id=p.patient_id"
		        + " left join" + " (   select pid1.* " + ", pid2.lUuid lUuid from patient_identifier pid1" + "	inner join" + "	("
		        + "		select patient_id,min(patient_identifier_id) id, l.uuid lUuid" + "		from patient_identifier pi inner join location l on l.location_id = pi.location_id "
		        + "		where pi.voided=0 and pi.identifier_type=2 and l.retired=0 " + "		group by patient_id" + "	) pid2"
		        + "	where pid1.patient_id=pid2.patient_id and pid1.patient_identifier_id=pid2.id "
		        + ") pid on pid.patient_id=p.patient_id" + " left join" + " (	select pn1.*" + "	from person_name pn1"
		        + "	inner join" + "	(" + "		select person_id,min(person_name_id) id" + "		from person_name"
		        + "		where voided=0" + "		group by person_id" + "	) pn2"
		        + "	where pn1.person_id=pn2.person_id and pn1.person_name_id=pn2.id " + ") pn on pn.person_id=p.patient_id"
		        + " left join" + " (	select pa1.*" + "	from person_address pa1" + "	inner join" + "	("
		        + "		select person_id,min(person_address_id) id" + "		from person_address" + "		where voided=0"
		        + "		group by person_id" + "	) pa2" + "	where pa1.person_id=pa2.person_id and pa1.person_address_id=pa2.id "
		        + ") pa on pa.person_id=p.patient_id" + " left join" + " (	select pat1.*" + "	from person_attribute pat1"
		        + "	inner join" + "	(" + "		select person_id,min(person_attribute_id) id" + "		from person_attribute "
		        + "		where voided=0 and person_attribute_type_id = 9" + "		group by person_id" + "	) pat2"
		        + "	where pat1.person_id=pat2.person_id and pat1.person_attribute_id=pat2.id"
		        + ") pat on pat.person_id=p.patient_id" + " left join" + " (	select pat12.*"
		        + "	from person_attribute pat12" + "	inner join" + "	(" + "		select person_id,min(person_attribute_id) id"
		        + "		from person_attribute " + "		where voided=0 and person_attribute_type_id = 14" + "		group by person_id"
		        + "	) pat22" + "	where pat12.person_id=pat22.person_id and pat12.person_attribute_id=pat22.id "
		        + ") pat1 on pat1.person_id=p.patient_id" + " left join" + " (	select pat121.*"
		        + "	from person_attribute pat121" + "	inner join" + "	(" + "		select person_id,min(person_attribute_id) id"
		        + "		from person_attribute " + "		where voided=0 and person_attribute_type_id = 5" + "		group by person_id"
		        + "	) pat222" + "	where pat121.person_id=pat222.person_id and pat121.person_attribute_id=pat222.id "
		        + ") pat2 on pat2.person_id=p.patient_id "
		        + " where p.voided=0 and pe.voided=0 AND LENGTH(pid.identifier) = 21 AND pid.lUuid IN ("
		        + Util.listToString(locationsByUuid) + ") GROUP BY pid.identifier;";
				
				List<PatientDemographic> patientDemographics = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(PatientDemographic.class));
		
		return patientDemographics;
	}

}
