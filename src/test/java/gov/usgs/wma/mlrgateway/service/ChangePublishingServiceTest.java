package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.BaseSpringTest;
import static gov.usgs.wma.mlrgateway.BaseSpringTest.getAdd;
import gov.usgs.wma.mlrgateway.Creation;
import gov.usgs.wma.mlrgateway.Modification;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class ChangePublishingServiceTest extends BaseSpringTest {
	private static final String SNS_TOPIC_ARN = "arn:aws:sns:us-west-2:000000000001:mock";
	private static final ObjectMapper GOOD_OBJECT_MAPPER = new ObjectMapper();

	@Test
	public void testPublishAttachesAMessageDeduplicationId() {
		SnsClient client = mock(SnsClient.class);
		ChangePublishingService instance = new ChangePublishingService(
			client,
			SNS_TOPIC_ARN,
			GOOD_OBJECT_MAPPER
		);
		
		SiteReport siteReport = new SiteReport("USGS", "12345678");
		siteReport.setTransactionType("A");
		ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
		instance.publish(
			new Creation<>(getAdd()),
			siteReport
		);
		verify(client).publish(requestCaptor.capture());
		PublishRequest actualRequest = requestCaptor.getValue();
		assertNotNull(actualRequest.messageDeduplicationId());
	}
	
	@Test
	public void testSuccessfulCreationPublish() {
		SnsClient client = mock(SnsClient.class);
		ChangePublishingService instance = new ChangePublishingService(
			client,
			SNS_TOPIC_ARN,
			GOOD_OBJECT_MAPPER
		);
		
		SiteReport siteReport = new SiteReport("USGS", "12345678");
		siteReport.setTransactionType("A");
		instance.publish(
			new Creation<>(getAdd()),
			siteReport
		);
		verify(client).publish(any(PublishRequest.class));
		StepReport actualStepReport = siteReport.getSteps().get(0);
		assertEquals(
			ChangePublishingService.successfulStepReport(),
			actualStepReport
		);
	}
	
	@Test
	public void testSuccessfulModificationPublish() {
		SnsClient client = mock(SnsClient.class);
		ChangePublishingService instance = new ChangePublishingService(
			client,
			SNS_TOPIC_ARN,
			GOOD_OBJECT_MAPPER
		);
		
		SiteReport siteReport = new SiteReport("USGS", "12345678");
		siteReport.setTransactionType("M");
		instance.publish(
			new Modification<>(
				getAdd(),//before
				getUpdate()//after
			),
			siteReport
		);
		verify(client).publish(any(PublishRequest.class));
		StepReport actualStepReport = siteReport.getSteps().get(0);
		assertEquals(
			ChangePublishingService.successfulStepReport(),
			actualStepReport
		);
	}
	
	@Test
	public void testSerializationError() throws JsonProcessingException {
		SnsClient client = mock(SnsClient.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
		
		ChangePublishingService instance = new ChangePublishingService(
			client,
			SNS_TOPIC_ARN,
			objectMapper
		);
		
		SiteReport siteReport = new SiteReport("USGS", "12345678");
		siteReport.setTransactionType("A");
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			instance.publish(
				new Creation<>(getAdd()),
				siteReport
			);
		});
		
		assertEquals(ChangePublishingService.SERIALIZATION_ERROR, ex.getMessage());
		
		verify(client, never()).publish(any(PublishRequest.class));
		StepReport actualStepReport = siteReport.getSteps().get(0);
		assertEquals(
			ChangePublishingService.serializationErrorStepReport(),
			actualStepReport
		);
	}
	
	@Test
	public void testPublishingError() throws JsonProcessingException {
		SnsClient client = mock(SnsClient.class);
		when(client.publish(any(PublishRequest.class))).thenThrow(RuntimeException.class);
		ChangePublishingService instance = new ChangePublishingService(
			client,
			SNS_TOPIC_ARN,
			GOOD_OBJECT_MAPPER
		);

		SiteReport siteReport = new SiteReport("USGS", "12345678");
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			instance.publish(
				new Creation<>(getAdd()),
				siteReport
			);
		});

		assertEquals(ChangePublishingService.PUBLISHING_ERROR, ex.getMessage());

		verify(client).publish(any(PublishRequest.class));
		StepReport actualStepReport = siteReport.getSteps().get(0);
		assertEquals(
			ChangePublishingService.publishingErrorStepReport(),
			actualStepReport
		);
	}
}
