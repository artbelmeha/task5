package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role"
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		int principalId = (Integer) input.get("principalId");
		Map<String, String> content = (Map<String, String>) input.get("content");
		String createdAt = java.time.ZonedDateTime.now().toString();

		String id = UUID.randomUUID().toString();

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		Table table = dynamoDB.getTable("Events");

		Item item = new Item()
				.withPrimaryKey("id", id)
				.withNumber("principalId", principalId)
				.withString("createdAt", createdAt)
				.withMap("body", content);
		PutItemOutcome putItemOutcome = table.putItem(item);


		Map<String, Object> event = new HashMap<>();
		event.put("id", id);
		event.put("principalId", principalId);
		event.put("createdAt", createdAt);
		event.put("body", content);

		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 201);
		response.put("event", event);
		response.put("err", putItemOutcome);

		return response;
	}
}
