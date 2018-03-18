package io.crnk.core.engine.internal.dispatcher.controller;

import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.http.HttpStatus;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.dispatcher.path.ResourcePath;
import io.crnk.core.engine.internal.document.mapper.DocumentMapper;
import io.crnk.core.engine.internal.document.mapper.DocumentMappingConfig;
import io.crnk.core.engine.internal.repository.ResourceRepositoryAdapter;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.result.Result;
import io.crnk.core.repository.response.JsonApiResponse;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;

import java.util.Set;

public class ResourcePost extends ResourceUpsert {

	@Override
	protected HttpMethod getHttpMethod() {
		return HttpMethod.POST;
	}

	@Override
	public boolean isAcceptable(JsonPath jsonPath, String requestType) {
		return jsonPath.isCollection() && jsonPath instanceof ResourcePath && HttpMethod.POST.name().equals(requestType);
	}

	@Override
	public Result<Response> handleAsync(JsonPath jsonPath, QueryAdapter queryAdapter,
										RepositoryMethodParameterProvider parameterProvider, Document requestDocument) {

		RegistryEntry endpointRegistryEntry = getRegistryEntry(jsonPath);
		Resource requestResource = getRequestBody(requestDocument, jsonPath, HttpMethod.POST);
		RegistryEntry registryEntry = context.getResourceRegistry().getEntry(requestResource.getType());
		ResourceInformation resourceInformation = registryEntry.getResourceInformation();
		verifyTypes(HttpMethod.POST, endpointRegistryEntry, registryEntry);

		ResourceRepositoryAdapter resourceRepository = endpointRegistryEntry.getResourceRepository(parameterProvider);

		Set<String> loadedRelationshipNames = getLoadedRelationshipNames(requestResource);

		Result<JsonApiResponse> response;
		if (Resource.class.equals(resourceInformation.getResourceClass())) {
			response = resourceRepository.create(requestResource, queryAdapter);
		} else {
			Object newResource = newResource(registryEntry.getResourceInformation(), requestResource);
			setId(requestResource, newResource, registryEntry.getResourceInformation());
			setAttributes(requestResource, newResource, registryEntry.getResourceInformation());
			response = setRelationsAsync(newResource, registryEntry, requestResource, queryAdapter, parameterProvider, false)
					.merge(it -> resourceRepository.create(newResource, queryAdapter));
		}

		DocumentMappingConfig mappingConfig = DocumentMappingConfig.create()
				.setParameterProvider(parameterProvider)
				.setFieldsWithEnforcedIdSerialization(loadedRelationshipNames);
		DocumentMapper documentMapper = context.getDocumentMapper();

		return response.doWork(this::validateResponse)
				.merge(it -> documentMapper.toDocument(it, queryAdapter, mappingConfig))
				.map(this::toResponse);
	}

	private Response toResponse(Document document) {
		return new Response(document, HttpStatus.CREATED_201);
	}

	private void validateResponse(JsonApiResponse response) {
		if (response.getEntity() == null) {
			throw new IllegalStateException("repository did not return the created resource");
		}
	}

}
