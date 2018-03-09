package io.crnk.reactive.repository.filter;

import io.crnk.core.engine.filter.RepositoryFilterContext;
import io.crnk.core.resource.links.LinksInformation;

public interface ReactiveLinksFilterChain {


	<T> LinksInformation doFilter(RepositoryFilterContext context, Iterable<T> resources);

}
