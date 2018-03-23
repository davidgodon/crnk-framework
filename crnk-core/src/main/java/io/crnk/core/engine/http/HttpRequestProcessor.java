package io.crnk.core.engine.http;

import io.crnk.core.engine.result.Result;

import java.io.IOException;

public interface HttpRequestProcessor {

	default boolean supportsAsync() {
		return false;
	}

	default boolean accepts(HttpRequestContext context) {
		throw new UnsupportedOperationException("cannot be used in reactive setup");
	}

	@Deprecated
	default void process(HttpRequestContext context) throws IOException {
		HttpResponse response = processAsync(context).get();
		context.setResponse(response);
	}

	default Result<HttpResponse> processAsync(HttpRequestContext context) {
		throw new UnsupportedOperationException("cannot be used in reactive setup");
	}
}
