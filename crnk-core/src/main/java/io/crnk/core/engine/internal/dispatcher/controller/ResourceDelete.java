package io.crnk.core.engine.internal.dispatcher.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.dispatcher.path.PathIds;
import io.crnk.core.engine.internal.dispatcher.path.ResourcePath;
import io.crnk.core.engine.internal.repository.ResourceRepositoryAdapter;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.result.Result;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.repository.response.JsonApiResponse;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;

public class ResourceDelete extends BaseController {

	@Override
	public boolean isAcceptable(JsonPath jsonPath, String requestType) {
		return !jsonPath.isCollection()
				&& jsonPath instanceof ResourcePath
				&& HttpMethod.DELETE.name().equals(requestType);
	}

	@Override
	public Result<Response> handleAsync(JsonPath jsonPath, QueryAdapter queryAdapter,
			RepositoryMethodParameterProvider parameterProvider, Document requestBody) {
		String resourceName = jsonPath.getElementName();
		PathIds resourceIds = jsonPath.getIds();
		RegistryEntry registryEntry = context.getResourceRegistry().getEntry(resourceName);
		if (registryEntry == null) {
			//TODO: Add JsonPath toString and provide to exception?
			throw new ResourceNotFoundException(resourceName);
		}

		List<Result<JsonApiResponse>> results = new ArrayList<>();
		for (String id : resourceIds.getIds()) {
			Serializable castedId = registryEntry.getResourceInformation().parseIdString(id);
			ResourceRepositoryAdapter resourceRepository = registryEntry.getResourceRepository(parameterProvider);
			Result<JsonApiResponse> result = resourceRepository.delete(castedId, queryAdapter);
			results.add(result);
		}

		return context.getResultFactory().all(results).map(it -> new Response(null, 204));
	}
}
