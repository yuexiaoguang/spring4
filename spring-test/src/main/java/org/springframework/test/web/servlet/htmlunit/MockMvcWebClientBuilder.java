package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebClient;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcWebClientBuilder}简化了委托给{@link MockMvc}实例的HtmlUnit {@link WebClient}的创建.
 *
 * <p>构建器使用的{@code MockMvc}实例可能是{@linkplain #mockMvcSetup 直接提供}
 * 或透明地从{@link #webAppContextSetup WebApplicationContext}创建.
 */
public class MockMvcWebClientBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcWebClientBuilder> {

	private WebClient webClient;


	protected MockMvcWebClientBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}


	/**
	 * @param mockMvc 要使用的{@code MockMvc}实例; never {@code null}
	 * 
	 * @return 要自定义的MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcWebClientBuilder(mockMvc);
	}

	/**
	 * @param context 从中创建{@link MockMvc}实例的{@code WebApplicationContext}; never {@code null}
	 * 
	 * @return 要自定义的MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcWebClientBuilder(context);
	}

	/**
	 * @param context 从中创建{@link MockMvc}实例的{@code WebApplicationContext}; never {@code null}
	 * @param configurer 要应用的{@code MockMvcConfigurer}; never {@code null}
	 * 
	 * @return 要自定义的MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcWebClientBuilder(context, configurer);
	}

	/**
	 * 在处理非{@linkplain WebRequestMatcher 匹配}请求时,
	 * 提供此构建器应该委托的客户端{@linkplain #build 构建}的{@code WebClient}.
	 * 
	 * @param webClient 不匹配的请求委托给的{@code WebClient}; never {@code null}
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	public MockMvcWebClientBuilder withDelegate(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		webClient.setWebConnection(createConnection(webClient));
		this.webClient = webClient;
		return this;
	}

	/**
	 * 构建通过此构建器配置的{@link WebClient}.
	 * <p>返回的客户端将使用配置的{@link MockMvc}实例处理任何{@linkplain WebRequestMatcher 匹配}请求,
	 * 并使用委托{@code WebClient}处理所有其他请求.
	 * <p>如果已明确配置{@linkplain #withDelegate 委托}, 则将使用它; 否则, 默认的{@code WebClient}将被配置为委托.
	 * 
	 * @return 要使用的{@code WebClient}
	 */
	public WebClient build() {
		return (this.webClient != null ? this.webClient : withDelegate(new WebClient()).build());
	}

}
