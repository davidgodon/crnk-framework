package io.crnk.reactive.internal.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.http.HttpRequestContext;
import io.crnk.core.engine.internal.dispatcher.ControllerRegistry;
import io.crnk.core.engine.internal.dispatcher.controller.BaseController;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.http.JsonApiRequestProcessor;
import io.crnk.core.engine.internal.http.JsonApiRequestProcessorBase;
import io.crnk.core.engine.query.QueryAdapterBuilder;
import io.crnk.core.module.Module;
import io.crnk.reactive.engine.document.ReactiveDocumentFilterChain;
import io.crnk.reactive.engine.http.ReactiveHttpRequestProcessor;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

public class ReactiveJsonApiRequestProcessor extends JsonApiRequestProcessorBase implements ReactiveHttpRequestProcessor {

	public ReactiveJsonApiRequestProcessor(Module.ModuleContext moduleContext, ControllerRegistry controllerRegistry,
										   QueryAdapterBuilder queryAdapterBuilder) {
		super(moduleContext, queryAdapterBuilder, controllerRegistry);
	}

	@Override
	public boolean accepts(HttpRequestContext requestContext) {
		return JsonApiRequestProcessor.isJsonApiRequest(requestContext, isAcceptingPlainJson());
	}

	@Override
	public Mono<?> process(HttpRequestContext requestContext) {
		JsonPath jsonPath = getJsonPath(requestContext);
		String method = requestContext.getMethod();
		Map<String, Set<String>> parameters = requestContext.getRequestParameters();
		Document requestDocument;
		try {
			requestDocument = getRequestDocument(requestContext);
		} catch (JsonProcessingException e) {
			Response response = setJsonError(requestContext, e);
			return Mono.just(response);
		}

		ReactiveDocumentFilterChain chain = getFilterChain(jsonPath, method);
		DocumentFilterContext context = getFilterContext(jsonPath, method, parameters, null, requestDocument);
		return chain.doFilter(context);
	}


	protected ReactiveDocumentFilterChain getFilterChain(JsonPath jsonPath, String method) {
		BaseController controller = controllerRegistry.getController(jsonPath, method);
		return new ReactiveDocumentFilterChainImpl(moduleContext, controller);

	}
}
