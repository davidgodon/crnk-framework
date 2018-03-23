package io.crnk.home;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.filter.FilterBehavior;
import io.crnk.core.engine.http.*;
import io.crnk.core.engine.information.repository.RepositoryInformation;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.dispatcher.path.PathBuilder;
import io.crnk.core.engine.internal.utils.UrlUtils;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.result.Result;
import io.crnk.core.engine.result.ResultFactory;
import io.crnk.core.module.Module;
import io.crnk.core.module.ModuleExtensionAware;

import java.io.IOException;
import java.net.HttpRetryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays a list of available resources in the root directory.
 */
public class HomeModule implements Module, ModuleExtensionAware<HomeModuleExtension> {

	public static final String JSON_HOME_CONTENT_TYPE = "application/json-home";

	public static final String JSON_CONTENT_TYPE = "application/json";

	private List<HomeModuleExtension> extensions;

	private HomeFormat defaultFormat;

	private ModuleContext moduleContext;

	// protected for CDI
	protected HomeModule() {
	}

	public static HomeModule create() {
		return create(HomeFormat.JSON_API);
	}

	public static HomeModule create(HomeFormat format) {
		HomeModule module = new HomeModule();
		module.defaultFormat = format;
		return module;
	}


	@Override
	public String getModuleName() {
		return "home";
	}

	@Override
	public void setupModule(final ModuleContext context) {
		this.moduleContext = context;
		context.addHttpRequestProcessor(new HttpRequestProcessor() {

			@Override
			public boolean supportsAsync() {
				return true;
			}

			@Override
			public boolean accepts(HttpRequestContext requestContext) {
				String path = requestContext.getPath();
				if (!path.endsWith("/") || !requestContext.getMethod().equalsIgnoreCase(HttpMethod.GET.toString())) {
					return false;
				}

				boolean acceptsHome = requestContext.accepts(JSON_HOME_CONTENT_TYPE);
				boolean acceptsJsonApi = requestContext.accepts(HttpHeaders.JSONAPI_CONTENT_TYPE);
				boolean acceptsJson = requestContext.accepts(HttpHeaders.JSON_CONTENT_TYPE);
				boolean acceptsAny = requestContext.acceptsAny();
				if (!(acceptsHome || acceptsAny || acceptsJson || acceptsJsonApi)) {
					return false;
				}

				ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();
				JsonPath jsonPath = new PathBuilder(resourceRegistry).build(path);
				return jsonPath == null; // no repository with that path

			}

			@Override
			public Result<HttpResponse> processAsync(HttpRequestContext requestContext) {
				Set<String> pathSet = new HashSet<>();

				ResourceRegistry resourceRegistry = context.getResourceRegistry();
				for (RegistryEntry resourceEntry : resourceRegistry.getResources()) {
					RepositoryInformation repositoryInformation = resourceEntry.getRepositoryInformation();
					if (repositoryInformation != null &&
							context.getResourceFilterDirectory().get(resourceEntry.getResourceInformation(), HttpMethod.GET)
									== FilterBehavior.NONE) {
						ResourceInformation resourceInformation = resourceEntry.getResourceInformation();
						String resourceType = resourceInformation.getResourceType();
						pathSet.add("/" + resourceType);
					}
				}

				if (extensions != null) {
					for (HomeModuleExtension extension : extensions) {
						pathSet.addAll(extension.getPaths());
					}
				}

				String requestPath = requestContext.getPath();
				pathSet = pathSet.stream().map(it -> getChildName(requestPath, it))
						.filter(it -> it != null).collect(Collectors.toSet());

				List<String> pathList = new ArrayList<>(pathSet);
				Collections.sort(pathList);

				HttpResponse response;
				if (defaultFormat == HomeFormat.JSON_HOME || requestContext.accepts(JSON_HOME_CONTENT_TYPE)) {
					boolean acceptsHome = requestContext.accepts(JSON_HOME_CONTENT_TYPE);
					response = getResponse(requestContext, pathList);
					if (acceptsHome) {
						response.setContentType(JSON_HOME_CONTENT_TYPE);
					} else {
						response.setContentType(JSON_CONTENT_TYPE);
					}
				} else {
					boolean jsonapi = requestContext.accepts(HttpHeaders.JSONAPI_CONTENT_TYPE);
					response = getResponse(requestContext, pathList);
					if (jsonapi) {
						response.setContentType(HttpHeaders.JSONAPI_CONTENT_TYPE);
					} else {
						response.setContentType(JSON_CONTENT_TYPE);
					}
				}

				ResultFactory resultFactory = moduleContext.getResultFactory();
				return resultFactory.just(response);
			}

			private String getChildName(String requestPath, String it) {
				if (it.startsWith(requestPath)) {
					String subPath = UrlUtils.removeTrailingSlash(it.substring(requestPath.length()));
					int sep = subPath.indexOf('/');
					return sep == -1 ? subPath : subPath.substring(0, sep) + "/";
				}
				return null;
			}
		});
	}

	private HttpResponse getResponse(HttpRequestContext requestContext, List<String> pathList) {
		ObjectMapper objectMapper = moduleContext.getObjectMapper();

		String baseUrl = UrlUtils.removeTrailingSlash(moduleContext.getResourceRegistry().getServiceUrlProvider().getUrl())
				+ requestContext.getPath();

		ObjectNode node = objectMapper.createObjectNode();
		ObjectNode links = node.putObject("links");
		for (String path : pathList) {
			String href = baseUrl + path;
			String id = UrlUtils.removeTrailingSlash(path);
			links.put(id, href);
		}

		String json;
		try {
			json = objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}

		HttpResponse response = new HttpResponse();
		response.setStatusCode(200);
		response.setBody(json);
		return response;
	}

	private void writeJsonHome(HttpRequestContext requestContext, List<String> pathList) throws IOException {
		ObjectMapper objectMapper = moduleContext.getObjectMapper();

		ObjectNode node = objectMapper.createObjectNode();
		ObjectNode resourcesNode = node.putObject("resources");
		for (String path : pathList) {
			String tag = "tag:" + UrlUtils.removeTrailingSlash(path);
			String href = path;
			ObjectNode resourceNode = resourcesNode.putObject(tag);
			resourceNode.put("href", href);
		}
		String json = objectMapper.writeValueAsString(node);
		System.out.println(json);
		requestContext.setResponse(200, json);
	}


	@Override
	public void setExtensions(List<HomeModuleExtension> extensions) {
		this.extensions = extensions;
	}

	@Override
	public void init() {

	}
}
