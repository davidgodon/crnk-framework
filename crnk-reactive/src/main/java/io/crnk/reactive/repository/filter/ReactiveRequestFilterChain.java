package io.crnk.reactive.repository.filter;

import io.crnk.core.engine.filter.RepositoryFilterContext;
import io.crnk.core.repository.response.JsonApiResponse;

public interface ReactiveRequestFilterChain {


	JsonApiResponse doFilter(RepositoryFilterContext context);

}
