package io.crnk.core.engine.internal.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.crnk.core.engine.dispatcher.RequestDispatcher;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.error.JsonApiExceptionMapper;
import io.crnk.core.engine.filter.DocumentFilterChain;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.http.HttpHeaders;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.http.HttpRequestContext;
import io.crnk.core.engine.http.HttpRequestProcessor;
import io.crnk.core.engine.internal.dispatcher.ControllerRegistry;
import io.crnk.core.engine.internal.dispatcher.controller.BaseController;
import io.crnk.core.engine.internal.dispatcher.path.ActionPath;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.exception.ExceptionMapperRegistry;
import io.crnk.core.engine.query.QueryAdapterBuilder;
import io.crnk.core.module.Module;
import io.crnk.core.utils.Optional;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
	public void process(HttpRequestContext requestContext) throws IOException {
		if (isJsonApiRequest(requestContext, isAcceptingPlainJson())) {
			RequestDispatcher requestDispatcher = moduleContext.getRequestDispatcher();

			JsonPath jsonPath = getJsonPath(requestContext);
			Map<String, Set<String>> parameters = requestContext.getRequestParameters();
			String method = requestContext.getMethod();

			String path = requestContext.getPath();
			if (jsonPath instanceof ActionPath) {
				// inital implementation, has to improve
				requestDispatcher.dispatchAction(path, method, parameters);
			} else if (jsonPath != null) {

				Document document;
				try {
					document = getRequestDocument(requestContext);
				} catch (JsonProcessingException e) {
					setJsonError(requestContext, e);
					return;
				}

				RepositoryMethodParameterProvider parameterProvider = requestContext.getRequestParameterProvider();
				Response crnkResponse = requestDispatcher
						.dispatchRequest(path, method, parameters, parameterProvider, document);
				setResponse(requestContext, crnkResponse);
			} else {
				// no repositories invoked, we do nothing
			}
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
		BaseController controller = controllerRegistry.getController(jsonPath, method);
		return new DocumentFilterChainImpl(moduleContext, controller);

	}
}
