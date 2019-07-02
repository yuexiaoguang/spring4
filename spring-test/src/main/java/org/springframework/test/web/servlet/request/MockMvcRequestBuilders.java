package org.springframework.test.web.servlet.request;

import java.net.URI;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * {@link RequestBuilder RequestBuilders}的静态工厂方法.
 *
 * <h3>与Spring TestContext Framework集成</h3>
 * <p>此类中的方法将重用由Spring TestContext Framework创建的
 * {@link org.springframework.mock.web.MockServletContext MockServletContext}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to this setting, open the Preferences and type "favorites".
 */
public abstract class MockMvcRequestBuilders {

	/**
	 * 用于GET请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder get(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * 用于GET请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder get(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
	}

	/**
	 * 用于POST请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * 用于POST请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder post(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, uri);
	}

	/**
	 * 用于PUT请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder put(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * 用于PUT请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder put(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, uri);
	}

	/**
	 * 用于PATCH请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder patch(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * 用于PATCH请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder patch(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, uri);
	}

	/**
	 * 用于DELETE请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder delete(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * 用于DELETE请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder delete(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, uri);
	}

	/**
	 * 用于OPTIONS请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder options(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * 用于OPTIONS请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder options(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, uri);
	}

	/**
	 * 用于HEAD请求.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder head(String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * 用于HEAD请求.
	 * 
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder head(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, uri);
	}

	/**
	 * 使用指定的HTTP方法.
	 * 
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod method, String urlTemplate, Object... uriVars) {
		return new MockHttpServletRequestBuilder(method, urlTemplate, uriVars);
	}

	/**
	 * 使用指定的HTTP方法.
	 * 
	 * @param httpMethod HTTP方法 (GET, POST, etc)
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * 允许自定义HTTP谓词的替代工厂方法 (e.g. WebDAV).
	 * 
	 * @param httpMethod HTTP方法
	 * @param uri the URL
	 */
	public static MockHttpServletRequestBuilder request(String httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVars 零个或多个URI变量
	 */
	public static MockMultipartHttpServletRequestBuilder fileUpload(String urlTemplate, Object... uriVars) {
		return new MockMultipartHttpServletRequestBuilder(urlTemplate, uriVars);
	}

	/**
	 * @param uri the URL
	 */
	public static MockMultipartHttpServletRequestBuilder fileUpload(URI uri) {
		return new MockMultipartHttpServletRequestBuilder(uri);
	}


	/**
	 * 从启动异步处理的请求的{@link MvcResult}创建用于异步调度的{@link RequestBuilder}.
	 * <p>用法涉及执行首先启动异步处理的请求:
	 * <pre class="code">
	 * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
	 *	.andExpect(request().asyncStarted())
	 *	.andReturn();
	 *  </pre>
	 * <p>然后使用{@code MvcResult}重新执行异步调度:
	 * <pre class="code">
	 * this.mockMvc.perform(asyncDispatch(mvcResult))
	 * 	.andExpect(status().isOk())
	 * 	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 * 	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	 * </pre>
	 * 
	 * @param mvcResult 来自启动异步处理的请求的结果
	 */
	public static RequestBuilder asyncDispatch(final MvcResult mvcResult) {

		// 在分派之前必须有异步结果
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
