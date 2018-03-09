package io.crnk.reactive.repository.filter;

import io.crnk.core.engine.filter.RepositoryFilterContext;

public interface ReactiveResultFilterChain<T> {

	Iterable<T> doFilter(RepositoryFilterContext context);

}
