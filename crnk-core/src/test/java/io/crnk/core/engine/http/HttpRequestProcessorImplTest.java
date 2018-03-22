package io.crnk.core.engine.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.dispatcher.RequestDispatcher;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.filter.AbstractDocumentFilter;
import io.crnk.core.engine.filter.DocumentFilter;
import io.crnk.core.engine.filter.DocumentFilterChain;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.information.repository.RepositoryAction;
import io.crnk.core.engine.information.repository.RepositoryInformation;
import io.crnk.core.engine.information.repository.RepositoryInformationProvider;
import io.crnk.core.engine.information.repository.RepositoryInformationProviderContext;
import io.crnk.core.engine.information.repository.RepositoryMethodAccess;
import io.crnk.core.engine.information.resource.ResourceAction;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.ControllerRegistry;
import io.crnk.core.engine.internal.dispatcher.controller.CollectionGet;
import io.crnk.core.engine.internal.dispatcher.controller.RelationshipsResourceGet;
import io.crnk.core.engine.internal.dispatcher.path.ActionPath;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.exception.ExceptionMapperRegistryTest;
import io.crnk.core.engine.internal.http.HttpRequestContextBaseAdapter;
import io.crnk.core.engine.internal.http.HttpRequestProcessorImpl;
import io.crnk.core.engine.internal.http.JsonApiRequestProcessor;
import io.crnk.core.engine.internal.information.repository.ResourceRepositoryInformationImpl;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.result.SimpleResult;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.mock.MockConstants;
import io.crnk.core.module.Module;
import io.crnk.core.module.ModuleRegistry;
import io.crnk.core.module.discovery.ReflectionsServiceDiscovery;
import io.crnk.core.repository.ResourceRepositoryV2;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class HttpRequestProcessorImplTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private ModuleRegistry moduleRegistry;

	private DocumentFilter documentFilter = Mockito.spy(AbstractDocumentFilter.class);

	private CrnkBoot boot;

	private JsonApiRequestProcessor requestProcessor;

	@Before
	public void prepare() {
		boot = new CrnkBoot();
		boot.setServiceDiscovery(new ReflectionsServiceDiscovery(MockConstants.TEST_MODELS_PACKAGE));
		boot.addModule(new ActionTestModule());
		boot.boot();

		moduleRegistry = boot.getModuleRegistry();

		requestProcessor = (JsonApiRequestProcessor) boot.getModuleRegistry().getHttpRequestProcessors().get(0);
	}

	@JsonApiResource(type = "actionResource")
	public static class ActionResource {

		@JsonApiId
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	/**
	 * Testing for a repository with actions (currently the only actual implementation is with JAXRS).
	 */
	class ActionTestModule implements Module {

		@Override
		public String getModuleName() {
			return "actionTest";
		}

		@Override
		public void setupModule(ModuleContext context) {

			final ResourceRepositoryV2 mockRepository = Mockito.mock(ResourceRepositoryV2.class);
			Mockito.when(mockRepository.getResourceClass()).thenReturn(ActionResource.class);

			context.addFilter(documentFilter);
			context.addRepository(mockRepository);
			context.addRepositoryInformationBuilder(new RepositoryInformationProvider() {
				@Override
				public boolean accept(Class<?> repositoryClass) {
					return false;
				}

				@Override
				public boolean accept(Object repository) {
					return repository == mockRepository;
				}

				@Override
				public RepositoryInformation build(Object repository, RepositoryInformationProviderContext context) {

					ResourceInformation resourceInformation = context.getResourceInformationBuilder().build(ActionResource
							.class);

					HashMap<String, RepositoryAction> actions = new HashMap<>();
					ResourceAction action = Mockito.mock(ResourceAction.class);
					Mockito.when(action.getActionType()).thenReturn(RepositoryAction.RepositoryActionType.RESOURCE);
					actions.put("someAction", action);
					RepositoryInformation repositoryInformation = new ResourceRepositoryInformationImpl("actionResource",
							resourceInformation, actions, RepositoryMethodAccess.ALL);
					return repositoryInformation;

				}

				@Override
				public RepositoryInformation build(Class<?> repositoryClass, RepositoryInformationProviderContext context) {
					return null;
				}
			});
		}
	}


	@Test
	public void checkProcess() throws IOException {
		HttpRequestContextBase requestContextBase = Mockito.mock(HttpRequestContextBase.class);
		HttpRequestContextBaseAdapter requestContext = new HttpRequestContextBaseAdapter(requestContextBase);

		Mockito.when(requestContextBase.getMethod()).thenReturn("GET");
		Mockito.when(requestContextBase.getPath()).thenReturn("/tasks/");
		Mockito.when(requestContextBase.getRequestHeader("Accept")).thenReturn("*");

		CollectionGet controller = mock(CollectionGet.class);
		when(controller.isAcceptable(any(JsonPath.class), eq("GET"))).thenCallRealMethod();

		when(controller.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
				any(Document.class))).thenReturn(new SimpleResult<>(null));

		ControllerRegistry controllerRegistry = boot.getControllerRegistry();
		controllerRegistry.getControllers().clear();
		controllerRegistry.addController(controller);

		RequestDispatcher sut = new HttpRequestProcessorImpl(moduleRegistry, null);
		sut.process(requestContext);

		verify(controller, times(1))
				.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
						any(Document.class));
	}

	@Test
	public void onGivenPathAndRequestTypeControllerShouldHandleRequest() throws Exception {
		// GIVEN
		String path = "/tasks/";
		String requestType = "GET";

		CollectionGet controller = mock(CollectionGet.class);
		ControllerRegistry controllerRegistry = boot.getControllerRegistry();
		controllerRegistry.getControllers().clear();
		controllerRegistry.addController(controller);

		RequestDispatcher sut = new HttpRequestProcessorImpl(moduleRegistry, null);

		// WHEN
		when(controller.isAcceptable(any(JsonPath.class), eq(requestType))).thenCallRealMethod();
		when(controller.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
				any(Document.class))).thenReturn(new SimpleResult<>(null));

		Map<String, Set<String>> parameters = new HashMap<>();
		sut.dispatchRequest(path, requestType, parameters, null, null);

		// THEN
		verify(controller, times(1))
				.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
						any(Document.class));
	}

	@Test
	public void shouldHandleRelationshipRequest() throws Exception {
		// GIVEN
		String path = "/tasks/1/relationships/project";
		String requestType = "GET";

		RelationshipsResourceGet controller = mock(RelationshipsResourceGet.class);
		ControllerRegistry controllerRegistry = boot.getControllerRegistry();
		controllerRegistry.getControllers().clear();
		controllerRegistry.addController(controller);

		RequestDispatcher sut = new HttpRequestProcessorImpl(moduleRegistry, null);

		// WHEN
		when(controller.isAcceptable(any(JsonPath.class), eq(requestType))).thenCallRealMethod();
		when(controller.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
				any(Document.class))).thenReturn(new SimpleResult<>(null));

		Map<String, Set<String>> parameters = new HashMap<>();
		sut.dispatchRequest(path, requestType, parameters, null, null);

		// THEN
		verify(controller, times(1))
				.handleAsync(any(JsonPath.class), any(QueryAdapter.class), any(RepositoryMethodParameterProvider.class),
						any(Document.class));
	}

	@Test
	public void shouldNotifyWhenActionIsExeecuted() throws Exception {
		// GIVEN
		String path = "/actionResource/1/someAction";
		String requestType = "GET";

		RequestDispatcher sut = new HttpRequestProcessorImpl(moduleRegistry, null);

		// WHEN
		Map<String, Set<String>> parameters = new HashMap<>();
		sut.dispatchAction(path, "GET", parameters);

		// THEN

		ArgumentCaptor<DocumentFilterContext> filterContextCaptor = ArgumentCaptor.forClass(DocumentFilterContext.class);

		Mockito.verify(documentFilter, Mockito.times(1)).filter(filterContextCaptor.capture(), Mockito.any
				(DocumentFilterChain.class));
		DocumentFilterContext filterContext = filterContextCaptor.getValue();
		Assert.assertEquals("GET", filterContext.getMethod());
		Assert.assertTrue(filterContext.getJsonPath() instanceof ActionPath);
	}


	@Test
	public void shouldMapExceptionToErrorResponseIfMapperIsAvailable() throws Exception {
		ControllerRegistry controllerRegistry = mock(ControllerRegistry.class);
		// noinspection unchecked
		when(controllerRegistry.getController(any(JsonPath.class), anyString())).thenThrow(new BadRequestException("test"));
		requestProcessor.setControllerRegistry(controllerRegistry);

		RequestDispatcher requestDispatcher = new HttpRequestProcessorImpl(moduleRegistry,
				ExceptionMapperRegistryTest.exceptionMapperRegistry);

		Response response = requestDispatcher.dispatchRequest("tasks", null, null, null, null);
		assertThat(response).isNotNull();
		assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
	}

	@Test
	public void shouldThrowExceptionAsIsIfMapperIsNotAvailable() throws Exception {
		ControllerRegistry controllerRegistry = mock(ControllerRegistry.class);
		// noinspection unchecked
		when(controllerRegistry.getController(any(JsonPath.class), anyString())).thenThrow(ArithmeticException.class);
		requestProcessor.setControllerRegistry(controllerRegistry);

		RequestDispatcher
				requestDispatcher =
				new HttpRequestProcessorImpl(moduleRegistry, ExceptionMapperRegistryTest.exceptionMapperRegistry);

		expectedException.expect(ArithmeticException.class);

		requestDispatcher.dispatchRequest("tasks", null, null, null, null);
	}
}
