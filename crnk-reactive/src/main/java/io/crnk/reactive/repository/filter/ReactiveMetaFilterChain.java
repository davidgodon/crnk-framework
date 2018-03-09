package io.crnk.reactive.repository.filter;

import io.crnk.core.engine.filter.RepositoryFilterContext;
import io.crnk.core.resource.meta.MetaInformation;

public interface ReactiveMetaFilterChain {


	<T> MetaInformation doFilter(RepositoryFilterContext context, Iterable<T> resources);

}
