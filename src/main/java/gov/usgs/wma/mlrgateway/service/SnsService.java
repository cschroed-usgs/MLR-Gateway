package gov.usgs.wma.mlrgateway.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class SnsService {
	private final SnsClient client;

	//constructor for runtime
	public SnsService() {
		client = SnsClient.create();
	}
	
	//constructor for unit tests
	public SnsService(SnsClient client) {
		this.client = client;
	}
	
	/**
	 * 
	 * This method leverages the AWS SDK's default exponential retry logic 
	 * https://docs.aws.amazon.com/general/latest/gr/api-retries.html
	 * 
	 * @param message
	 * @param topicArn
	 * @param groupId 
	 */
	public void publish(String message, String topicArn, String groupId) {
		//Our SNS does not implement content-based deduplication, so we must provide our own unique deduplication ids
		String dedupId = UUID.randomUUID().toString();
		PublishRequest request = PublishRequest.builder()
			.message(message)
			.topicArn(topicArn)
			.messageGroupId(groupId)
			.messageDeduplicationId(dedupId)
			.build();
		client.publish(request);
	}
}
