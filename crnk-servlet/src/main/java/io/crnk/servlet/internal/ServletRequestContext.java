/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.crnk.servlet.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.crnk.core.engine.http.HttpRequestContextBase;
import io.crnk.core.engine.http.HttpResponse;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.engine.internal.utils.UrlUtils;
import io.crnk.core.utils.Nullable;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;
import io.crnk.servlet.internal.legacy.ServletParametersProvider;

public class ServletRequestContext implements HttpRequestContextBase {


	private final HttpServletRequest servletRequest;

	private final HttpServletResponse servletResponse;

	private final ServletParametersProvider parameterProvider;

	private final ServletContext servletContext;

	private Map<String, Set<String>> parameters;

	private Nullable<byte[]> requestBody = Nullable.empty();

	private HttpResponse response = new HttpResponse();

	private String pathPrefix;

	public ServletRequestContext(final ServletContext servletContext, final HttpServletRequest request,
								 final HttpServletResponse response, String pathPrefix) {
		this.pathPrefix = pathPrefix;
		this.servletContext = servletContext;
		this.servletRequest = request;
		this.servletResponse = response;
		this.parameterProvider = new ServletParametersProvider(servletContext, request, response);
		this.parameters = getParameters(request);
	}


	public boolean checkAbort() throws IOException {
		if (response.getStatusCode() > 0) {
			servletResponse.setStatus(response.getStatusCode());
			if (response.getBody() != null) {
				OutputStream out = servletResponse.getOutputStream();
				out.write(response.getBody());
				out.close();
			}
			return true;
		}
		return false;
	}

	private Map<String, Set<String>> getParameters(HttpServletRequest request) {
		Map<String, Set<String>> queryParameters = new HashMap<>();
		for (Map.Entry<String, String[]> queryEntry : request.getParameterMap().entrySet()) {
			queryParameters.put(queryEntry.getKey(), new LinkedHashSet<>(Arrays.asList(queryEntry.getValue())));
		}
		return queryParameters;
	}


	@Override
	public RepositoryMethodParameterProvider getRequestParameterProvider() {
		return parameterProvider;
	}

	@Override
	public String getRequestHeader(String name) {
		return servletRequest.getHeader(name);
	}

	@Override
	public Map<String, Set<String>> getRequestParameters() {
		return parameters;
	}

	@Override
	public String getPath() {
		String path = servletRequest.getPathInfo();

		// Serving with Filter, pathInfo can be null.
		if (path == null) {
			path = servletRequest.getRequestURI().substring(servletRequest.getContextPath().length());
		}

		if (pathPrefix != null && path.startsWith(pathPrefix)) {
			path = path.substring(pathPrefix.length());
		}

		if (path.isEmpty()) {
			return "/";
		}

		return path;
	}

	@Override
	public String getBaseUrl() {
		String requestUrl = servletRequest.getRequestURL().toString();
		String servletPath = servletRequest.getServletPath();
		int sep = requestUrl.indexOf(servletPath);

		if (pathPrefix != null && servletPath.startsWith(pathPrefix)) {
			servletPath = pathPrefix;
		} else if (servletPath.isEmpty()) {
			return UrlUtils.removeTrailingSlash(requestUrl);
		}

		String url = requestUrl.substring(0, sep + servletPath.length());
		return UrlUtils.removeTrailingSlash(url);
	}

	@Override
	public byte[] getRequestBody() {
		if (!requestBody.isPresent()) {
			try {
				InputStream is = servletRequest.getInputStream();
				if (is != null) {
					requestBody = Nullable.of(io.crnk.core.engine.internal.utils.IOUtils.readFully(is));
				} else {
					requestBody = Nullable.nullValue();
				}
			} catch (IOException e) {
				throw new IllegalStateException(e); // FIXME
			}
		}
		return requestBody.get();
	}

	@Override
	public void setResponseHeader(String name, String value) {
		response.setHeader(name, value);
	}

	@Override
	public void setResponse(int code, byte[] body) {
		response.setStatusCode(code);
		response.setBody(body);
	}

	@Override
	public String getMethod() {
		return servletRequest.getMethod().toUpperCase();
	}

	@Override
	public String getResponseHeader(String name) {
		return response.getHeader(name);
	}

	@Override
	public HttpResponse getResponse() {
		return response;
	}

	@Override
	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	/**
	 * @deprecated use {{@link #getResponseHeader(String)}}
	 */
	@Deprecated
	public HttpServletRequest getRequest() {
		return servletRequest;
	}

	public HttpServletRequest getServletRequest() {
		return servletRequest;
	}

	public HttpServletResponse getServletResponse() {
		return servletResponse;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}
}
