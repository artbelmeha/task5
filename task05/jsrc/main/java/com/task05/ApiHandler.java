package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role"
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	private final AmazonDynamoDB dynamoDB;

	public ApiHandler() {
		dynamoDB = AmazonDynamoDBClient.builder()
				.withRegion("eu-central-1")
				.build();
	}
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

		Map<String, AttributeValue> item = getObject(input);
		PutItemRequest putItemRequest = new PutItemRequest("cmtr-76c36f18-Events-test", item);
		dynamoDB.putItem(putItemRequest);

		Map<String, Object> event = new HashMap<>();
		event.put("id", item.get("id"));
		event.put("principalId", item.get("principalId"));
		event.put("createdAt", item.get("createdAt"));
		event.put("body", input.get("body"));

		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 201);
		response.put("event", event);
		return response;
	}

	public Map<String, AttributeValue> getObject(Map<String, Object> input) {
		Map<String, AttributeValue> item = new HashMap<>();
		Map<String, String> content = (Map<String, String>) input.get("content");
		item.put("id", new AttributeValue(UUID.randomUUID().toString()));
		item.put("principalId", new AttributeValue().withN((String)input.get("principalId")));
		item.put("createdAt", new AttributeValue().withS(java.time.ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)));
		item.put("body", new AttributeValue().withM(content.entrySet()
						.stream()
						.collect(Collectors.toMap(Entry::getKey, e->new AttributeValue(e.getValue())))));
		return item;
	}
}
