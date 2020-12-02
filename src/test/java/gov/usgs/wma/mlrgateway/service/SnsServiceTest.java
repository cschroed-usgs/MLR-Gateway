package gov.usgs.wma.mlrgateway.service;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
public class SnsServiceTest {
	
	public SnsServiceTest() {
	}

	private SnsService instance;
	private SnsClient client;
	private ArgumentCaptor<PublishRequest> requestCaptor;
	
	@BeforeEach
	public void setUp() {
		client = mock(SnsClient.class);
		instance = new SnsService();
		requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
	}
	@Test
	public void testPublishAttachesAMessageDeduplicationId() {
		instance.publish(
			"some message",
			"arn:aws:sns:us-west-2:000000000001:mock",
			"some group"
		);
		verify(client).publish(requestCaptor.capture());
		PublishRequest actualRequest = requestCaptor.getValue();
		assertNotNull(actualRequest.messageDeduplicationId());
	}
}
