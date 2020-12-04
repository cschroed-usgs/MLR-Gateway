package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.Change;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import java.util.Map;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class ChangePublishingService {
	
	private final SnsClient snsClient;
	private final ObjectMapper objectMapper;
	private final String snsChangeTopicArn;
	
	public static final String GROUP_ID= "monitoring-location-changes";
	public static final String STEP_NAME = "Publish Change To Other WMA Cloud Apps";
	public static final String STEP_SUCCESS = "Successfully published change to monitoring location";
	public static final String SERIALIZATION_ERROR = "{\"error_message\": \"Failed to serialize change. Aborted before publishing.\"}";
	public static final String PUBLISHING_ERROR = "{\"error_message\": \"Failed while publishing.\"}";
	
	private static final Logger LOG = LoggerFactory.getLogger(ChangePublishingService.class);
	
	//Don't change this constant without a really good reason. Monitoring tools look for this. If you change it, ask App Support to coordinate changes to the monitoring tools
	private static final String ERROR_LOG_MESSAGE_PREFIX = "FAILED TO PUBLISH MONITORING LOCATION CHANGE TO SNS:";
	
	//Runtime constructor
	@Autowired
	public ChangePublishingService(@Qualifier("SnsChangeTopicArn")String snsChangeTopicArn) {
		this(SnsClient.create(), snsChangeTopicArn, new ObjectMapper());
	}
	
	//Test-time constructor
	public ChangePublishingService(SnsClient snsClient, String snsChangeTopicArn, ObjectMapper objectMapper) {
		this.snsClient = snsClient;
		this.snsChangeTopicArn = snsChangeTopicArn;
		this.objectMapper = objectMapper;
	}
	
	public void publish(Change<?> change, SiteReport siteReport) {
		String deduplicationId = UUID.randomUUID().toString();
		try {
			String serializedChange = objectMapper.writeValueAsString(change);
			try {
				PublishRequest request = PublishRequest.builder()
					.message(serializedChange)
					.topicArn(snsChangeTopicArn)
					.messageGroupId(GROUP_ID)
					.messageDeduplicationId(deduplicationId)
					.build();
				snsClient.publish(request);
				LOG.debug(
					"Successfully published change\tTopic ARN=%s\tGroup ID=%s\tDeduplication Id=%s\tMessage=%s",
					snsChangeTopicArn,
					GROUP_ID,
					deduplicationId,
					serializedChange
				);
				siteReport.addStepReport(successfulStepReport());
			} catch (Exception ex) {
				String msg = String.format(
					"%s An error occurred while publishing to SNS.\tTopic ARN=%s\tGroup ID=%s\tDeduplication Id=%s\tMessage=%s",
					ERROR_LOG_MESSAGE_PREFIX,
					snsChangeTopicArn,
					GROUP_ID,
					deduplicationId,
					serializedChange
				);
				LOG.error(msg, ex);
				siteReport.addStepReport(publishingErrorStepReport());
			}
		} catch (JsonProcessingException ex) {
			String msg = String.format(
				"%s Unable to serialize changes for %s %s %s. Aborting prior to publishing changes.",
				ERROR_LOG_MESSAGE_PREFIX,
				siteReport.getAgencyCode(),
				siteReport.getSiteNumber(),
				siteReport.getTransactionType()
			);
			LOG.error(msg, ex);
			siteReport.addStepReport(serializationErrorStepReport());
		}
	}

	public static StepReport successfulStepReport() {
		return new StepReport(
			STEP_NAME,
			HttpStatus.SC_OK,
			true,
			STEP_SUCCESS
		);
	}
	
	public static StepReport serializationErrorStepReport() {
		return new StepReport(
			STEP_NAME,
			HttpStatus.SC_BAD_REQUEST,
			false,
			SERIALIZATION_ERROR
		);
	}
	
	public static StepReport publishingErrorStepReport() {
		return new StepReport(
			STEP_NAME,
			HttpStatus.SC_INTERNAL_SERVER_ERROR,
			false,
			PUBLISHING_ERROR
		);
	}
}
