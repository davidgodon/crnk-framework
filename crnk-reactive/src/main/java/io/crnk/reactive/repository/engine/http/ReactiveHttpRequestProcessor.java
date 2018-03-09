package io.crnk.reactive.repository.engine.http;

import io.crnk.core.engine.http.HttpRequestContext;
import java.io.IOException;
import reactor.core.publisher.Mono;

public interface ReactiveHttpRequestProcessor {

	Mono<Void> process(HttpRequestContext context) throws IOException;
}
