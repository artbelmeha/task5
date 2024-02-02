package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.time.Instant;
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
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final AmazonDynamoDB dynamoDB;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApiHandler() {
		dynamoDB = AmazonDynamoDBClient.builder()
				.withRegion("eu-central-1")
				.build();
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		Request request = getRequest(event.getBody());
		Response response = generateApiResponse(request);
		PutItemRequest putItemRequest = new PutItemRequest("Events",
				toDynamoDBItem(response));
		dynamoDB.putItem(putItemRequest);
		APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
		responseEvent.setStatusCode(HttpStatus.SC_CREATED);
		responseEvent.setBody(parseResponse(response));
		return responseEvent;
	}

	@SneakyThrows
	public Request getRequest(String event) {
		return objectMapper.readValue(event, Request.class);
	}

	@SneakyThrows
	public String parseResponse(Response response) {
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
						Instant.now().toString(),
						request.getContent()));
	}

	public Map<String, AttributeValue> toDynamoDBItem(Response response) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(response.getEvent().getId()));
		item.put("principalId", new AttributeValue().withN(response.getEvent().getPrincipalId().toString()));
		item.put("createdAt", new AttributeValue().withS(response.getEvent().getCreatedAt()));
		item.put("body", new AttributeValue().withM(parseContent(response.getEvent().getBody())));
		return item;
	}
}