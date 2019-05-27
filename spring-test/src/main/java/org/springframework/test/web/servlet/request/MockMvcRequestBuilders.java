package org.springframework.test.web.servlet.request;

import java.net.URI;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Static factory methods for {@link RequestBuilder RequestBuilders}.
 *
 * <h3>Integration with the Spring TestContext Framework</h3>
 * <p>Methods in this class will reuse a
 * {@link org.springframework.mock.web.MockServletContext MockServletContext}
 * that was created by the Spring TestContext Framework.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 */
public abstract class MockMvcRequestBuilders {

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a GET request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder get(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a GET request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder get(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a POST request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a POST request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder post(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder put(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder put(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder patch(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder patch(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder delete(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder delete(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder options(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder options(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
	 * @param uri the URL
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod method, String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(method, urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
	 * @param httpMethod the HTTP method (GET, POST, etc)
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * Alternative factory method that allows for custom HTTP verbs (e.g. WebDAV).
	 * @param httpMethod the HTTP method
	 * @param uri the URL
	 * @since 4.3
	 */
	public static MockHttpServletRequestBuilder request(String httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * Create a {@link MockMultipartHttpServletRequestBuilder} for a multipart request.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 */
	public static MockMultipartHttpServletRequestBuilder fileUpload(String urlTemplate, Object... uriVars) {
		return new MockMultipartHttpServletRequestBuilder(urlTemplate, uriVars);
	}

	/**
	 * Create a {@link MockMultipartHttpServletRequestBuilder} for a multipart request.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockMultipartHttpServletRequestBuilder fileUpload(URI uri) {
		return new MockMultipartHttpServletRequestBuilder(uri);
	}


	/**
	 * Create a {@link RequestBuilder} for an async dispatch from the
	 * {@link MvcResult} of the request that started async processing.
	 * <p>Usage involves performing a request that starts async processing first:
	 * <pre class="code">
	 * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
	 *	.andExpect(request().asyncStarted())
	 *	.andReturn();
	 *  </pre>
	 * <p>And then performing the async dispatch re-using the {@code MvcResult}:
	 * <pre class="code">
	 * this.mockMvc.perform(asyncDispatch(mvcResult))
	 * 	.andExpect(status().isOk())
	 * 	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 * 	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	 * </pre>
	 * @param mvcResult the result from the request that started async processing
	 */
	public static RequestBuilder asyncDispatch(final MvcResult mvcResult) {

		// There must be an async result before dispatching
		mvcResult.getAsyncResult();

		return new RequestBuilder() {
			@Override
			public MockHttpServletRequest buildRequest(ServletContext servletContext) {
				MockHttpServletRequest request = mvcResult.getRequest();
				request.setDispatcherType(DispatcherType.ASYNC);
				request.setAsyncStarted(false);
				return request;
			}
		};
	}

}
