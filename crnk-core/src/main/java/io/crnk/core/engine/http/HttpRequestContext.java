package io.crnk.core.engine.http;

public interface HttpRequestContext extends HttpRequestContextBase {

	boolean accepts(String contentType);

	void setContentType(String contentType);

	void setResponse(int statusCode, String text);

	boolean acceptsAny();

	<T> T unwrap(Class<T> type);

	Object getRequestAttribute(String name);

	void setRequestAttribute(String name, Object value);

}
