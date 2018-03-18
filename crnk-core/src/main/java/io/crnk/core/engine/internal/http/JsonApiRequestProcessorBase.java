package io.crnk.core.engine.internal.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.boot.CrnkProperties;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.http.HttpHeaders;
import io.crnk.core.engine.http.HttpRequestContext;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.ControllerRegistry;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.dispatcher.path.PathBuilder;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.query.QueryAdapterBuilder;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.exception.ResourceFieldNotFoundException;
import io.crnk.core.module.Module;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class JsonApiRequestProcessorBase {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected Module.ModuleContext moduleContext;

	private Boolean acceptingPlainJson;

	protected QueryAdapterBuilder queryAdapterBuilder;

	protected ControllerRegistry controllerRegistry;

	public JsonApiRequestProcessorBase(Module.ModuleContext moduleContext, QueryAdapterBuilder queryAdapterBuilder, ControllerRegistry controllerRegistry) {
		this.moduleContext = moduleContext;
		this.queryAdapterBuilder = queryAdapterBuilder;
		this.controllerRegistry = controllerRegistry;
	}

	protected DocumentFilterContext getFilterContext(JsonPath jsonPath, String method, Map<String, Set<String>> parameters,
													 RepositoryMethodParameterProvider parameterProvider,
													 Document requestBody) {
		ResourceInformation resourceInformation = getRequestedResource(jsonPath);
		QueryAdapter queryAdapter = queryAdapterBuilder.build(resourceInformation, parameters);
		return new DocumentFilterContextImpl(jsonPath, queryAdapter, parameterProvider,
				requestBody, method);
	}

	protected boolean isAcceptingPlainJson() {
		if (acceptingPlainJson == null) {
			acceptingPlainJson = !Boolean.parseBoolean(moduleContext.getPropertiesProvider().getProperty(CrnkProperties.REJECT_PLAIN_JSON));
		}
		return acceptingPlainJson;
	}

	protected Response setJsonError(HttpRequestContext requestContext, JsonProcessingException e) {
		final String message = "Json Parsing failed";
		Response response = buildBadRequestResponse(message, e.getMessage());
		setResponse(requestContext, response);
		logger.error(message, e);
		return response;
	}


	protected Document getRequestDocument(HttpRequestContext requestContext) throws JsonProcessingException {
		byte[] requestBody = requestContext.getRequestBody();
		if (requestBody != null && requestBody.length > 0) {
			ObjectMapper objectMapper = moduleContext.getObjectMapper();
			try {
				return objectMapper.readerFor(Document.class).readValue(requestBody);
			} catch (JsonProcessingException e) {
				throw e;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}


	protected Response buildBadRequestResponse(final String message, final String detail) {
		Document responseDocument = new Document();
		responseDocument.setErrors(Arrays.asList(ErrorData.builder()
				.setStatus(String.valueOf(400))
				.setTitle(message)
				.setDetail(detail)
				.build()));
		return new Response(responseDocument, 400);
	}

	protected void setResponse(HttpRequestContext requestContext, Response crnkResponse) {
		if (crnkResponse != null) {
			ObjectMapper objectMapper = moduleContext.getObjectMapper();
			String responseBody = null;
			try {
				responseBody = objectMapper.writeValueAsString(crnkResponse.getDocument());
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}

			requestContext.setResponseHeader("Content-Type", HttpHeaders.JSONAPI_CONTENT_TYPE_AND_CHARSET);
			requestContext.setResponse(crnkResponse.getHttpStatus(), responseBody);
		}
	}

	protected JsonPath getJsonPath(HttpRequestContext requestContext) {
		String path = requestContext.getPath();
		ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();
		return new PathBuilder(resourceRegistry).build(path);
	}

	protected ResourceInformation getRequestedResource(JsonPath jsonPath) {
		ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();
		RegistryEntry registryEntry = resourceRegistry.getEntry(jsonPath.getResourceType());
		PreconditionUtil.assertNotNull("repository not found, that should have been catched earlier", registryEntry);
		String elementName = jsonPath.getElementName();
		if (elementName != null && !elementName.equals(jsonPath.getResourceType())) {
			ResourceField relationshipField = registryEntry.getResourceInformation().findRelationshipFieldByName(elementName);
			if (relationshipField == null) {
				throw new ResourceFieldNotFoundException(elementName);
			}
			String oppositeResourceType = relationshipField.getOppositeResourceType();
			return resourceRegistry.getEntry(oppositeResourceType).getResourceInformation();
		} else {
			return registryEntry.getResourceInformation();
		}
	}
}
