package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import java.util.HashMap;
import java.util.Map;
import net.minidev.json.JSONObject;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
public class LegacyCruServiceTest extends BaseSpringTest {
	private String reportName = "TEST LEGACYCRU";
	private LegacyCruService service;
	private ObjectMapper mapper;
	private String legacyJson = "{}";
	
	@MockBean
	LegacyCruClient legacyCruClient;

	@Before
	public void init() {
		service = new LegacyCruService(legacyCruClient);
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}
	
	@Test
	public void addTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_ADD_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);

		service.addTransaction("USGS ", "12345678       ", "{}");
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
	}

	@Test
	public void addTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Site add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.createMonitoringLocation(anyString())).willThrow(new RuntimeException());
		
		try {
			service.addTransaction("USGS ", "12345678       ", "{}");
			fail("addTransaction did not throw an exception to its caller");
		} catch(Exception e) {}
		
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
	}
	
	@Test
	public void updateTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_UPDATE_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);

		service.updateTransaction("USGS ", "12345678       ", "{}");
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
	}

	@Test
	public void updateTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Site update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.patchMonitoringLocation(anyString())).willThrow(new RuntimeException());

		try {
			service.updateTransaction("USGS ", "12345678       ", "{}");
			fail("addTransaction did not throw an exception to its caller");
		} catch(Exception e) {}
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
	}
	
	@Test
	public void getLocationUpdate_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_GET_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_GET_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation("USGS ", "12345678       ", false);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationAdd_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_GET_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_GET_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation("USGS ", "12345678       ", true);

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationUpdate_nullSite () throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":404,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_GET_STEP + "\",\"status\":404,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_GET_DOES_NOT_EXIST)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
	
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.NOT_FOUND);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation("USGS ", "12345678       ", false);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationAdd_nullSite () throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"Duplicate agency code/site number check: " + LegacyCruService.SITE_GET_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_GET_DOES_NOT_EXIST)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
	
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.NOT_FOUND);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation("USGS ", "12345678       ", true);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void validateMonitoringLocation_MlSerializationError () throws Exception {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		given(mockMapper.writeValueAsString(any())).willThrow(JsonProcessingException.class);
		service.setMapper(mockMapper);
		
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_VALIDATE_STEP +"\",\"status\":500,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_VALIDATE_FAILED) 
				+ "\"}"
				+ "]}";
		try {
			service.validateMonitoringLocation(new HashMap<>());
			fail();
		} catch (FeignBadResponseWrapper e) {
			assertNotNull(e);
		}
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_CruValidationMessageDeserializationError () throws Exception {
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>("NOT VALID JSON", HttpStatus.INTERNAL_SERVER_ERROR));
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_VALIDATE_STEP +"\",\"status\":500,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_VALIDATE_FAILED) 
				+ "\"}"
				+ "]}";
		try {
			service.validateMonitoringLocation(new HashMap<>());
			fail();
		} catch (FeignBadResponseWrapper e) {
			assertNotNull(e);
		}
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_ValidationSuccessful () throws Exception {
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>("[]", HttpStatus.OK));
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_VALIDATE_STEP +"\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyCruService.SITE_VALIDATE_SUCCESSFUL) 
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"0123456789       \"}"
				+ "]}";
		Map<String, Object> input = new HashMap<>();
		input.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		input.put(LegacyWorkflowService.SITE_NUMBER, "0123456789       ");
		
		service.validateMonitoringLocation(input);
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_ValidationFailed () throws Exception {
		String validationErrorMessages = "[\"it was bad\",\"it was not good\"]";
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>(validationErrorMessages, HttpStatus.NOT_ACCEPTABLE));
		String msg = "{\"name\":\"" + reportName + "\",\"status\":406,\"steps\":["
				+ "{\"name\":\"" + LegacyCruService.SITE_VALIDATE_STEP +"\",\"status\":406,\"details\":\"" + JSONObject.escape(validationErrorMessages) 
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"0123456789       \"}"
				+ "]}";
		Map<String, Object> input = new HashMap<>();
		input.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		input.put(LegacyWorkflowService.SITE_NUMBER, "0123456789       ");
		
		service.validateMonitoringLocation(input);
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
}