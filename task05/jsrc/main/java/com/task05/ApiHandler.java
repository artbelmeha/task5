package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}"
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		DynamoDbClient dynamodb = DynamoDbClient.builder()
				.region(Region.EU_CENTRAL_1)
				.build();

		String uuid = UUID.randomUUID().toString();
		PutItemRequest request = PutItemRequest.builder()
				.tableName("Events")
				.item(createEventItem(uuid, input))
				.build();

		dynamodb.putItem(request);

		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 201);
		response.put("event", input);
		input.put("id", uuid);
		input.put("createdAt", LocalDateTime.now().toString());
		return response;
	}

	private Map<String, AttributeValue> createEventItem(String uuid, Map<String, Object> input) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", AttributeValue.builder().s(uuid).build());
		item.put("principalId", AttributeValue.builder().n(input.get("principalId").toString()).build());
		item.put("createdAt", AttributeValue.builder().s(LocalDateTime.now().toString()).build());

		Map<String, AttributeValue> contentMap = new HashMap<>();

		((Map<String, String>) input.get("content")).forEach((key, value) -> {
			contentMap.put(key, AttributeValue.builder().s(value).build());
		});

		item.put("body", AttributeValue.builder().m(contentMap).build());

		return item;
	}
}
