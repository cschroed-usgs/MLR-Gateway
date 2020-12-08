package gov.usgs.wma.mlrgateway.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import gov.usgs.wma.mlrgateway.Change;
import gov.usgs.wma.mlrgateway.Creation;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.Modification;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.ChangePublishingService;
import gov.usgs.wma.mlrgateway.service.DdotService;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;
import java.util.Collections;

@Service
public class LegacyWorkflowService {
	private static final Logger LOG = LoggerFactory.getLogger(LegacyWorkflowService.class);
	private DdotService ddotService;
	private LegacyTransformerService transformService;
	private LegacyValidatorService legacyValidatorService;
	private LegacyCruService legacyCruService;
	private FileExportService fileExportService;
	private ChangePublishingService changePublishingService;
	
	public static final String ID = "id";
	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String DISTRICT_CODE = "districtCode";
	public static final String COORDINATE_DATUM_CODE = "coordinateDatumCode";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String NEW_AGENCY_CODE = "newAgencyCode";
	public static final String NEW_SITE_NUMBER = "newSiteNumber";
	public static final String REQUESTER_NAME = "requesterName";	
	public static final String UPDATED = "updated";
	public static final String REASON_TEXT = "reasonText";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";

	public static final String BAD_TRANSACTION_TYPE = "Unable to determine transaction type.";
	
	public static final String COMPLETE_WORKFLOW = "Validate and Process D dot File Workflow";
	public static final String COMPLETE_WORKFLOW_SUCCESS = "Validate and Process D dot File Workflow completed";
	public static final String COMPLETE_WORKFLOW_FAILED = "Validate and Process D dot File Workflow failed";
	public static final String VALIDATE_DDOT_WORKFLOW = "Validate D dot File";
	public static final String VALIDATE_DDOT_WORKFLOW_FAILED = "Validate D dot File workflow failed";
	public static final String VALIDATE_DDOT_WORKFLOW_SUCCESS = "Validate D dot File workflow completed";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW = "Site Agency Code and/or Site Number Update Workflow";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_SUCCESS = "Site Agency Code and/or Site Number Update workflow completed";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_FAILED = "Site Agency Code and/or Site Number Update workflow failed";
	
	public static final String VALIDATE_DDOT_TRANSACTION_STEP = "Validate Single D dot Transaction";
	public static final String VALIDATE_DDOT_TRANSACTION_STEP_FAILURE = "Single transaction validation failed.";
	public static final String COMPLETE_TRANSACTION_STEP = "Process Single D dot Transaction";
	public static final String PRIMARY_KEY_UPDATE_TRANSACTION_STEP = "Update Agency Code and/or Site Number";
	
	@Autowired
	public LegacyWorkflowService(DdotService ddotService, LegacyCruService legacyCruService, LegacyTransformerService transformService, 
			LegacyValidatorService legacyValidatorService, FileExportService fileExportService, ChangePublishingService changePublishingService) {
		this.ddotService = ddotService;
		this.legacyCruService = legacyCruService;
		this.transformService = transformService;
		this.legacyValidatorService = legacyValidatorService;
		this.fileExportService = fileExportService;
		this.changePublishingService = changePublishingService;
	}

	public void completeWorkflow(MultipartFile file) throws HystrixBadRequestException {
		String json;
		
		//1. Parse Ddot File
		LOG.trace("Start DDOT Parsing");
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		LOG.trace("End DDOT Parsing");

		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			LOG.trace("Start processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
			Map<String, Object> ml = ddots.get(i);
			Map<String, Object> existingRecord = new HashMap<>();
			SiteReport siteReport = new SiteReport(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString());
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					Boolean isAddTransaction = ((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD);
					siteReport.setTransactionType(ml.get(TRANSACTION_TYPE).toString());
					ml = transformService.transformStationIx(ml, siteReport);
					
					//Fetch Existing Record
					existingRecord = fetchExistingRecord(ml, isAddTransaction, siteReport);
					
					ml = legacyValidatorService.doValidation(ml, existingRecord, isAddTransaction, siteReport);
					
					// If this is an update, and latitude and longitude are being updated but the 
					// datum is not included, pull that from the existing record
					if (!isAddTransaction && ml.get(COORDINATE_DATUM_CODE) ==  null && ml.get(LATITUDE) != null && ml.get(LONGITUDE) != null ) {
						ml.put(COORDINATE_DATUM_CODE, existingRecord.get(COORDINATE_DATUM_CODE));
					}
					
					ml = transformService.transformGeo(ml, siteReport);
					json = mlToJson(ml);
					Change<Map<String, Object>> change;
					if (isAddTransaction) {
						json = legacyCruService.addTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json, siteReport);
						fileExportService.exportAdd(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString(), json, siteReport);
						change = new Creation<>(ml);
					} else {
						json = legacyCruService.patchTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json, siteReport);
						fileExportService.exportUpdate(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString(), json, siteReport);
						change = new Modification<>(existingRecord, ml);
					}
					changePublishingService.publish(change, siteReport);
					WorkflowController.addSiteReport(siteReport);
				} else {
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					LOG.debug("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
					WorkflowController.addSiteReport(siteReport);
				} else {
					LOG.error("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
					WorkflowController.addSiteReport(siteReport);
				}
			}
			LOG.trace("End processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
		}
	}

	public void ddotValidation(MultipartFile file) throws HystrixBadRequestException {
		//1. Parse Ddot File
		LOG.trace("Start DDOT Parsing");
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		LOG.trace("End DDOT Parsing");
		
		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			LOG.trace("Start processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
			Map<String, Object> ml = ddots.get(i);
			Map<String, Object> existingRecord = new HashMap<>();
			SiteReport siteReport = new SiteReport(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString());
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					siteReport.setTransactionType(ml.get(TRANSACTION_TYPE).toString());
					ml = transformService.transformStationIx(ml, siteReport);
					
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						//Fetch Existing Record
						existingRecord = fetchExistingRecord(ml, true, siteReport);
						ml = legacyValidatorService.doValidation(ml, existingRecord, true, siteReport);
					} else {
						existingRecord = fetchExistingRecord(ml, false, siteReport);
						ml = legacyValidatorService.doValidation(ml, existingRecord, false, siteReport);
					}
				} else {
					siteReport.addStepReport(new StepReport(LegacyValidatorService.VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, false, BAD_TRANSACTION_TYPE));
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}		
				
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					LOG.debug("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP , ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
				} else {
					LOG.error("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
				}
			}
			WorkflowController.addSiteReport(siteReport);
			LOG.trace("End processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
		}
	}
	
	public void updatePrimaryKeyWorkflow(String oldAgencyCode, String oldSiteNumber, String newAgencyCode, String newSiteNumber, String reasonText) throws HystrixBadRequestException {
		String json;
		Map<String, Object> previousMonitoringLocation = new HashMap<>();
		Map<String, Object> monitoringLocation = new HashMap<>();
		Map<String, Object> monitoringLocationToValidate = new HashMap<>();
		Map<String, Object> updatedMonitoringLocation = new HashMap<>();
		Map<String, Object> exportChangeObject = new HashMap<>();
		Map<String, Object> existingRecord = new HashMap<>();
		SiteReport oldSiteReport = new SiteReport(oldAgencyCode, oldSiteNumber);
		SiteReport newSiteReport = new SiteReport(newAgencyCode, newSiteNumber);
		// TODO: This might change to a new transaction type once we figure out what the new transaction file needs to look like
		oldSiteReport.setTransactionType("M");
		newSiteReport.setTransactionType("M");
		
		LOG.trace("Start processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");
		
		try {
			previousMonitoringLocation = Collections.unmodifiableMap(legacyCruService.getMonitoringLocation(oldAgencyCode, oldSiteNumber, false, oldSiteReport));
			monitoringLocation = new HashMap<>(previousMonitoringLocation);
			WorkflowController.addSiteReport(oldSiteReport);
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper){
				LOG.debug("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				oldSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
				WorkflowController.addSiteReport(oldSiteReport);
			} else {
				LOG.error("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				oldSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
				WorkflowController.addSiteReport(oldSiteReport);
			}
		}
		try {
			if (!monitoringLocation.isEmpty()) {
				
				// TODO: This might change to a new transaction type once we figure out what the new transaction file needs to look like
				monitoringLocation.put(TRANSACTION_TYPE, "M");
				
				monitoringLocation.replace(AGENCY_CODE, newAgencyCode);
				monitoringLocation.replace(SITE_NUMBER, newSiteNumber);
				
				// Just send relevant fields to the validator
				monitoringLocationToValidate.put(TRANSACTION_TYPE, "M");
				monitoringLocationToValidate.put(ID, monitoringLocation.get(ID));
				monitoringLocationToValidate.put(AGENCY_CODE, newAgencyCode);
				monitoringLocationToValidate.put(SITE_NUMBER, newSiteNumber);
				
				//Fetch Existing Record
				existingRecord = fetchExistingRecord(monitoringLocationToValidate, true, newSiteReport);
				
				monitoringLocationToValidate = legacyValidatorService.doPKValidation(monitoringLocationToValidate, existingRecord, newSiteReport);
				
				json = mlToJson(monitoringLocation);
				
				// Need to submit entire record for update (vs. patch), otherwise fields that are not submitted are set to null in the database.
				json = legacyCruService.updateTransaction(monitoringLocation.get(ID).toString(), json, newSiteReport);
				
				updatedMonitoringLocation = jsonToMl(json);
				
				exportChangeObject.put(AGENCY_CODE, oldAgencyCode);
				exportChangeObject.put(NEW_AGENCY_CODE, updatedMonitoringLocation.get(AGENCY_CODE));
				exportChangeObject.put(SITE_NUMBER, oldSiteNumber);
				exportChangeObject.put(NEW_SITE_NUMBER, updatedMonitoringLocation.get(SITE_NUMBER));
				exportChangeObject.put(REQUESTER_NAME, updatedMonitoringLocation.get("updatedBy"));
				exportChangeObject.put(UPDATED, updatedMonitoringLocation.get(UPDATED));
				exportChangeObject.put(REASON_TEXT, reasonText);
				
				json = mlToJson(exportChangeObject);
				
				fileExportService.exportChange(json, newSiteReport);
				changePublishingService.publish(
					new Modification<>(
						previousMonitoringLocation,
						monitoringLocation
					),
					newSiteReport
				);
				WorkflowController.addSiteReport(newSiteReport);
			}
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper){
				LOG.debug("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				newSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
				WorkflowController.addSiteReport(newSiteReport);
			} else {
				LOG.error("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				newSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
				WorkflowController.addSiteReport(newSiteReport);
			}
		}
		LOG.trace("End processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");
	}
	
	protected Map<String, Object> fetchExistingRecord(Map<String, Object> ml, Boolean isAddTransaction, SiteReport siteReport) {
		Map<String, Object> existingRecord = new HashMap<>();
		
		//Fetch Existing Record
		String siteNumber = ml.get(LegacyWorkflowService.SITE_NUMBER) != null ? ml.get(LegacyWorkflowService.SITE_NUMBER).toString() : null;
		String agencyCode = ml.get(LegacyWorkflowService.AGENCY_CODE) != null ? ml.get(LegacyWorkflowService.AGENCY_CODE).toString() : null;
		existingRecord = legacyCruService.getMonitoringLocation(agencyCode, siteNumber, isAddTransaction, siteReport);
		
		return existingRecord;
	}
	
	protected String mlToJson(Map<String, Object> ml) {
		ObjectMapper mapper = new ObjectMapper();
		String json = "{}";
		
		try {
			json = mapper.writeValueAsString(ml);
		} catch (Exception e) {
			LOG.error("Unable to serialize transformer output: ", e);
			// Unable to determine when this might actually happen, but the api says it can...
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize transformer output.\"}");
		}
		
		return json;
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> jsonToMl(String json) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> ml = new HashMap<>();
		
		try {
			ml = mapper.readValue(json, Map.class);
			
		} catch (Exception e) {
			// Unable to determine when this might actually happen, but the api says it can...
			LOG.error("Unable to serialize legacy cru output: ", e);
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize legacy cru output.\"}");
		}
		return ml;
	}
}
