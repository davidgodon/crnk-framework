package io.crnk.core.engine.internal.http;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.crnk.core.engine.dispatcher.RequestDispatcher;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.error.JsonApiExceptionMapper;
import io.crnk.core.engine.filter.DocumentFilterChain;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.http.*;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.ControllerRegistry;
import io.crnk.core.engine.internal.dispatcher.controller.Controller;
import io.crnk.core.engine.internal.dispatcher.path.ActionPath;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.exception.ExceptionMapperRegistry;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.query.QueryAdapterBuilder;
import io.crnk.core.engine.result.Result;
import io.crnk.core.engine.result.ResultFactory;
import io.crnk.core.module.Module;
import io.crnk.core.utils.Optional;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonApiRequestProcessor extends JsonApiRequestProcessorBase implements HttpRequestProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonApiRequestProcessor.class);


	public JsonApiRequestProcessor(Module.ModuleContext moduleContext, ControllerRegistry controllerRegistry,
								   QueryAdapterBuilder queryAdapterBuilder) {
		super(moduleContext, queryAdapterBuilder, controllerRegistry);
	}

	/**
	 * Determines whether the supplied HTTP request is considered a JSON-API request. Accepts plain JSON requests by default.
	 *
	 * @param requestContext The HTTP request
	 * @return <code>true</code> if it is a JSON-API request; <code>false</code> otherwise
	 * @see #isJsonApiRequest(HttpRequestContext, boolean)
	 */
	@Deprecated
	public static boolean isJsonApiRequest(HttpRequestContext requestContext) {
		return isJsonApiRequest(requestContext, true);
	}

	/**
	 * Determines whether the supplied HTTP request is considered a JSON-API request.
	 *
	 * @param requestContext  The HTTP request
	 * @param acceptPlainJson Whether a plain JSON request should also be considered a JSON-API request
	 * @return <code>true</code> if it is a JSON-API request; <code>false</code> otherwise
	 * @since 2.4
	 */
	@SuppressWarnings("UnnecessaryLocalVariable")
	public static boolean isJsonApiRequest(HttpRequestContext requestContext, boolean acceptPlainJson) {
		if (requestContext.getMethod().equalsIgnoreCase(HttpMethod.PATCH.toString()) || requestContext.getMethod()
				.equalsIgnoreCase(HttpMethod.POST.toString())) {
			String contentType = requestContext.getRequestHeader(HttpHeaders.HTTP_CONTENT_TYPE);
			if (contentType == null || !contentType.startsWith(HttpHeaders.JSONAPI_CONTENT_TYPE)) {
				return false;
			}
		}

		// short-circuit each of the possible Accept MIME type checks, so that we don't keep comparing after we have already
		// found a match. Intentionally kept as separate statements (instead of a big, chained ||) to ease debugging/maintenance.
		boolean acceptsJsonApi = requestContext.accepts(HttpHeaders.JSONAPI_CONTENT_TYPE);
		boolean acceptsAny = acceptsJsonApi || requestContext.acceptsAny();
		boolean acceptsPlainJson = acceptsAny || (acceptPlainJson && requestContext.accepts("application/json"));
		return acceptsPlainJson;
	}


	@Override
	public boolean supportsAsync() {
		return true;
	}

	@Override
	public boolean accepts(HttpRequestContext context) {
		return isJsonApiRequest(context, isAcceptingPlainJson());
	}

	@Override
	public Result<HttpResponse> processAsync(HttpRequestContext requestContext) {

		RequestDispatcher requestDispatcher = moduleContext.getRequestDispatcher();

		JsonPath jsonPath = getJsonPath(requestContext);
		Map<String, Set<String>> parameters = requestContext.getRequestParameters();
		String method = requestContext.getMethod();

		ResultFactory resultFactory = moduleContext.getResultFactory();

		String path = requestContext.getPath();
		if (jsonPath instanceof ActionPath) {
			// inital implementation, has to improve
			requestDispatcher.dispatchAction(path, method, parameters);
			return resultFactory.just(null);
		} else if (jsonPath != null) {
			Document document;
			try {
				document = getRequestDocument(requestContext);
			} catch (JsonProcessingException e) {
				return resultFactory.just(getErrorResponse(requestContext, e));
			}

			RepositoryMethodParameterProvider parameterProvider = requestContext.getRequestParameterProvider();

			Controller controller = controllerRegistry.getController(jsonPath, method);

			ResourceInformation resourceInformation = getRequestedResource(jsonPath);
			QueryAdapter queryAdapter = queryAdapterBuilder.build(resourceInformation, parameters);

			Result<Response> responseResult = controller.handleAsync(jsonPath, queryAdapter, parameterProvider, document);

			DocumentFilterContextImpl filterContext =  new DocumentFilterContextImpl(jsonPath, queryAdapter, parameterProvider, document, method);


			Result<Object> result = resultFactory.just(null);



			DocumentFilterChain filterChain = getFilterChain(jsonPath, method);
			return filterChain.doFilter(filterContext);
		}
	}


	public Response process(JsonPath jsonPath, String method, Map<String, Set<String>> parameters,
							RepositoryMethodParameterProvider parameterProvider,
							Document requestDocument) {
		try {
			DocumentFilterChain chain = getFilterChain(jsonPath, method);
			DocumentFilterContext context = getFilterContext(jsonPath, method, parameters, parameterProvider, requestDocument);
			return chain.doFilter(context);
		} catch (Exception e) {
			ExceptionMapperRegistry exceptionMapperRegistry = moduleContext.getExceptionMapperRegistry();
			Optional<JsonApiExceptionMapper> exceptionMapper = exceptionMapperRegistry.findMapperFor(e.getClass());
			if (exceptionMapper.isPresent()) {
				//noinspection unchecked
				LOGGER.debug("dispatching exception to mapper", e);
				return exceptionMapper.get().toErrorResponse(e).toResponse();
			} else {
				LOGGER.error("failed to process request", e);
				throw e;
			}
		}
	}

	protected DocumentFilterChain getFilterChain(JsonPath jsonPath, String method) {
		Controller controller = controllerRegistry.getController(jsonPath, method);
		return new DocumentFilterChainImpl(moduleContext, controller);

	}
}
