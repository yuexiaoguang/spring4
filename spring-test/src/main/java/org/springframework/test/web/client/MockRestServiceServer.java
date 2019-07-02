package org.springframework.test.web.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

/**
 * <strong>客户端REST测试的主要入口点</strong>.
 * 用于涉及直接或间接使用{@link RestTemplate}的测试.
 * 提供一种方法来设置将通过{@code RestTemplate}执行的预期请求, 以及发回的模拟响应, 从而消除对实际服务器的需求.
 *
 * <p>下面是一个假设从
 * {@code MockRestRequestMatchers}, {@code MockRestResponseCreators},
 * 和{@code ExpectedCount}进行静态导入的示例:
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
 *
 * server.expect(manyTimes(), requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * // Verify all expectations met
 * server.verify();
 * </pre>
 *
 * <p>请注意, 作为上述的替代方法, 还可以在{@code RestTemplate}上设置{@link MockMvcClientHttpRequestFactory},
 * 它允许针对{@link org.springframework.test.web.servlet.MockMvc MockMvc}实例执行请求.
 */
public class MockRestServiceServer {

	private final RequestExpectationManager expectationManager;


	/**
	 * 请参阅静态构建器方法和{@code createServer}快捷方法.
	 */
	private MockRestServiceServer(RequestExpectationManager expectationManager) {
		this.expectationManager = expectationManager;
	}


	/**
	 * 设置单个HTTP请求的期望.
	 * 返回的{@link ResponseActions}可用于设置进一步的期望以及定义响应.
	 * <p>在开始通过底层{@code RestTemplate}发出请求之前, 可以调用此方法任意次数, 以便设置所有预期的请求.
	 * 
	 * @param matcher 请求匹配器
	 * 
	 * @return 期望
	 */
	public ResponseActions expect(RequestMatcher matcher) {
		return expect(ExpectedCount.once(), matcher);
	}

	/**
	 * {@link #expect(RequestMatcher)}的替代方法, 指示预期执行请求的次数.
	 * <p>当请求期望具有大于1的预期计数时, 仅预期第一次执行与声明的顺序匹配.
	 * 此后可以在任何地方插入后续请求执行.
	 * 
	 * @param count 预期的数量
	 * @param matcher 请求匹配器
	 * 
	 * @return 期望
	 */
	public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
		return this.expectationManager.expectRequest(count, matcher);
	}

	/**
	 * 验证是否确实执行了通过{@link #expect(RequestMatcher)}设置的所有预期请求.
	 * 
	 * @throws AssertionError 当一些期望没有得到满足时
	 */
	public void verify() {
		this.expectationManager.verify();
	}

	/**
	 * 重置内部状态, 删除所有期望和记录的请求.
	 */
	public void reset() {
		this.expectationManager.reset();
	}


	/**
	 * 返回{@code MockRestServiceServer}的构建器, 该构建器应该用于回复给定的{@code RestTemplate}.
	 */
	public static MockRestServiceServerBuilder bindTo(RestTemplate restTemplate) {
		return new DefaultBuilder(restTemplate);
	}

	/**
	 * 返回{@code MockRestServiceServer}的构建器, 该构建器应该用于回复给定的{@code AsyncRestTemplate}.
	 */
	public static MockRestServiceServerBuilder bindTo(AsyncRestTemplate asyncRestTemplate) {
		return new DefaultBuilder(asyncRestTemplate);
	}

	/**
	 * 返回{@code MockRestServiceServer}的构建器, 该构建器应该用于回复给定的{@code RestGatewaySupport}.
	 */
	public static MockRestServiceServerBuilder bindTo(RestGatewaySupport restGateway) {
		Assert.notNull(restGateway, "'gatewaySupport' must not be null");
		return new DefaultBuilder(restGateway.getRestTemplate());
	}


	/**
	 * {@code bindTo(restTemplate).build()}的快捷方式.
	 * 
	 * @param restTemplate 模拟测试使用的RestTemplate
	 * 
	 * @return 模拟服务器
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		return bindTo(restTemplate).build();
	}

	/**
	 * {@code bindTo(asyncRestTemplate).build()}的快捷方式.
	 * 
	 * @param asyncRestTemplate 模拟测试使用的AsyncRestTemplate
	 * 
	 * @return 创建的模拟服务器
	 */
	public static MockRestServiceServer createServer(AsyncRestTemplate asyncRestTemplate) {
		return bindTo(asyncRestTemplate).build();
	}

	/**
	 * {@code bindTo(restGateway).build()}的快捷方式.
	 * 
	 * @param restGateway 模拟测试使用的REST网关
	 * 
	 * @return 创建的模拟服务器
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		return bindTo(restGateway).build();
	}


	/**
	 * 创建{@code MockRestServiceServer}的构建器.
	 */
	public interface MockRestServiceServerBuilder {

		/**
		 * 是否允许以任何顺序执行预期请求, 不一定与声明顺序匹配.
		 * <p>当设置为"true", 这实际上是一个快捷方式:<br>
		 * {@code builder.build(new UnorderedRequestExpectationManager)}.
		 * 
		 * @param ignoreExpectOrder 是否忽略期望的顺序
		 */
		MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder);

		/**
		 * 构建{@code MockRestServiceServer}, 并使用创建模拟请求的{@link ClientHttpRequestFactory}
		 * 设置底层{@code RestTemplate}或{@code AsyncRestTemplate}.
		 */
		MockRestServiceServer build();

		/**
		 * 一个重载的构建替代方案, 它接受自定义{@link RequestExpectationManager}.
		 */
		MockRestServiceServer build(RequestExpectationManager manager);
	}


	private static class DefaultBuilder implements MockRestServiceServerBuilder {

		private final RestTemplate restTemplate;

		private final AsyncRestTemplate asyncRestTemplate;

		private boolean ignoreExpectOrder;

		public DefaultBuilder(RestTemplate restTemplate) {
			Assert.notNull(restTemplate, "RestTemplate must not be null");
			this.restTemplate = restTemplate;
			this.asyncRestTemplate = null;
		}

		public DefaultBuilder(AsyncRestTemplate asyncRestTemplate) {
			Assert.notNull(asyncRestTemplate, "AsyncRestTemplate must not be null");
			this.restTemplate = null;
			this.asyncRestTemplate = asyncRestTemplate;
		}

		@Override
		public MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder) {
			this.ignoreExpectOrder = ignoreExpectOrder;
			return this;
		}

		@Override
		public MockRestServiceServer build() {
			if (this.ignoreExpectOrder) {
				return build(new UnorderedRequestExpectationManager());
			}
			else {
				return build(new SimpleRequestExpectationManager());
			}
		}

		@Override
		public MockRestServiceServer build(RequestExpectationManager manager) {
			MockRestServiceServer server = new MockRestServiceServer(manager);
			MockClientHttpRequestFactory factory = server.new MockClientHttpRequestFactory();
			if (this.restTemplate != null) {
				this.restTemplate.setRequestFactory(factory);
			}
			if (this.asyncRestTemplate != null) {
				this.asyncRestTemplate.setAsyncRequestFactory(factory);
			}
			return server;
		}
	}


	/**
	 * 通过迭代预期的{@link DefaultRequestExpectation}列表来创建请求的Mock ClientHttpRequestFactory.
	 */
	private class MockClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			return createRequestInternal(uri, httpMethod);
		}

		@Override
		public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
			return createRequestInternal(uri, httpMethod);
		}

		private MockAsyncClientHttpRequest createRequestInternal(URI uri, HttpMethod method) {
			Assert.notNull(uri, "'uri' must not be null");
			Assert.notNull(method, "'httpMethod' must not be null");

			return new MockAsyncClientHttpRequest(method, uri) {

				@Override
				protected ClientHttpResponse executeInternal() throws IOException {
					ClientHttpResponse response = expectationManager.validateRequest(this);
					setResponse(response);
					return response;
				}
			};
		}
	}
}
