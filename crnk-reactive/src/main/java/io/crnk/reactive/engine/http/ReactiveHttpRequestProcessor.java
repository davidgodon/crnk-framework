package io.crnk.reactive.engine.http;

import io.crnk.core.engine.http.HttpRequestContext;
import reactor.core.publisher.Mono;

public interface ReactiveHttpRequestProcessor {

	boolean accepts(HttpRequestContext requestContext);

	Mono<?> process(HttpRequestContext requestContext);

}
