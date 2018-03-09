package io.crnk.reactive.repository.filter;

import io.crnk.core.engine.filter.RepositoryFilterContext;
import io.crnk.core.repository.response.JsonApiResponse;
import io.crnk.core.resource.links.LinksInformation;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.MetaInformation;
import reactor.core.publisher.Mono;

public interface ReactiveRepositoryFilter {

	Mono<JsonApiResponse> filterRequest(RepositoryFilterContext context, ReactiveRequestFilterChain chain);

	<T> Mono<ResourceList<T>> filterResult(RepositoryFilterContext context, ReactiveResultFilterChain<T> chain);

	<T> Mono<MetaInformation> filterMeta(RepositoryFilterContext context, Iterable<T> resources, ReactiveMetaFilterChain
			chain);

	<T> Mono<LinksInformation> filterLinks(RepositoryFilterContext context, Iterable<T> resources, ReactiveLinksFilterChain
			chain);

}
