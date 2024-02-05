package com.task05;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role"
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE,
		name = "Events")
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final ObjectMapper objectMapper = new ObjectMapper();


	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.EU_CENTRAL_1)
				.build();
		Request request = getRequest(event.getBody());
		Response response = generateApiResponse(request);
		PutItemRequest putItemRequest = new PutItemRequest("Events",
				toDynamoDBItem(response));
		dynamoDB.putItem(putItemRequest);
		APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
		responseEvent.setStatusCode(HttpStatus.SC_CREATED);
		responseEvent.setBody(getResponse(response));
		return responseEvent;
	}

	@SneakyThrows
	public Request getRequest(String event) {
		return objectMapper.readValue(event, Request.class);
	}

	@SneakyThrows
	public String getResponse(Response response) {
		return objectMapper.writeValueAsString(response);
	}

	public Map<String, AttributeValue> parseContent(Map<String, String> content) {
		return content.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry::getKey, e->new AttributeValue(e.getValue())));
	}

	public Response generateApiResponse(Request request) {
		return new Response(201,
				new Event(UUID.randomUUID().toString(),
						request.getPrincipalId(),
						ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
						request.getContent()));
	}

	public Map<String, AttributeValue> toDynamoDBItem(Response response) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(response.getEvent().getId()));
		item.put("principalId", new AttributeValue().withN(String.valueOf(response.getEvent().getPrincipalId())));
		item.put("createdAt", new AttributeValue().withS(response.getEvent().getCreatedAt()));
		item.put("body", new AttributeValue().withM(parseContent(response.getEvent().getBody())));
		return item;
	}
}