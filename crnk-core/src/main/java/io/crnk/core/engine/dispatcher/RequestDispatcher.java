package io.crnk.core.engine.dispatcher;

import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.http.HttpRequestContextBase;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public interface RequestDispatcher {

	void process(HttpRequestContextBase requestContextBase) throws IOException;

	Future<Void> processAsync(HttpRequestContextBase requestContextBase) throws IOException;

	/**
	 * @deprecated make use of JsonApiRequestProcessor
	 */
	@Deprecated
	Response dispatchRequest(String jsonPath, String method, Map<String, Set<String>> parameters,
							 RepositoryMethodParameterProvider parameterProvider,
							 Document requestBody);

	/**
	 * @deprecated make use of JsonApiRequestProcessor
	 */
	@Deprecated
	void dispatchAction(String jsonPath, String method, Map<String, Set<String>> parameters);
}
