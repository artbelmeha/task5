package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}"
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
	private DynamoDB dynamoDb = new DynamoDB(client);

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		Table table = dynamoDb.getTable("Events");

		String eventId = UUID.randomUUID().toString();
		String createdAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
		int principalId = Integer.parseInt(event.getHeaders().get("principalId"));
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> content = new HashMap<>();
		try {
			content = mapper.readValue(event.getBody(), Map.class);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Item item = new Item().withPrimaryKey("id", eventId)
				.withString("createdAt", createdAt)
				.withInt("principalId", principalId)
				.withMap("body", content);

		PutItemOutcome outcome = table.putItem(item);

		response.setStatusCode(201);
		response.setBody(item.toJSON());

		return response;
	}
}
