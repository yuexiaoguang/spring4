package org.springframework.test.web.servlet.htmlunit;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * 支持类, 简化了创建使用{@link MockMvc}的{@link WebConnection}, 并可选择委托给真正的{@link WebConnection}以获取特定请求.
 *
 * <p>默认情况下, 对{@code localhost}的请求使用{@link MockMvc}, 否则使用真正的{@link WebConnection}.
 */
public abstract class MockMvcWebConnectionBuilderSupport<T extends MockMvcWebConnectionBuilderSupport<T>> {

	private final MockMvc mockMvc;

	private final List<WebRequestMatcher> requestMatchers = new ArrayList<WebRequestMatcher>();

	private String contextPath = "";

	private boolean alwaysUseMockMvc;


	/**
	 * @param mockMvc 要使用的{@code MockMvc}实例; never {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
		this.requestMatchers.add(new HostRequestMatcher("localhost"));
	}

	/**
	 * @param context 用于创建{@code MockMvc}实例的{@code WebApplicationContext}; never {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context) {
		this(MockMvcBuilders.webAppContextSetup(context).build());
	}

	/**
	 * @param context 用于创建{@code MockMvc}实例的{@code WebApplicationContext}; never {@code null}
	 * @param configurer 要应用的MockMvcConfigurer; never {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context, MockMvcConfigurer configurer) {
		this(MockMvcBuilders.webAppContextSetup(context).apply(configurer).build());
	}


	/**
	 * 设置要使用的上下文路径.
	 * <p>如果提供的值为{@code null}或为空, 则假定请求URL的第一个路径段为上下文路径.
	 * <p>默认 {@code ""}.
	 * 
	 * @param contextPath 要使用的上下文路径
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	@SuppressWarnings("unchecked")
	public T contextPath(String contextPath) {
		this.contextPath = contextPath;
		return (T) this;
	}

	/**
	 * 指定始终应使用{@link MockMvc}, 无论请求是什么样的.
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	@SuppressWarnings("unchecked")
	public T alwaysUseMockMvc() {
		this.alwaysUseMockMvc = true;
		return (T) this;
	}

	/**
	 * 添加额外的{@link WebRequestMatcher}实例, 以确保{@link MockMvc}用于处理请求, 如果此类匹配器与Web请求匹配.
	 * 
	 * @param matchers 额外的{@code WebRequestMatcher}实例
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvc(WebRequestMatcher... matchers) {
		for (WebRequestMatcher matcher : matchers) {
			this.requestMatchers.add(matcher);
		}
		return (T) this;
	}

	/**
	 * 如果提供的主机匹配, 则添加返回{@code true}的其他{@link WebRequestMatcher}实例 &mdash;
	 * 例如, {@code "example.com"}或{@code "example.com:8080"}.
	 * 
	 * @param hosts 确保{@code MockMvc}被调用的其他主机
	 * 
	 * @return 用于进一步自定义的构建器
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvcForHosts(String... hosts) {
		this.requestMatchers.add(new HostRequestMatcher(hosts));
		return (T) this;
	}

	/**
	 * 如果指定的{@link WebRequestMatcher}实例之一匹配, 则将创建使用{@link MockMvc}实例的新{@link WebConnection}.
	 * 
	 * @param defaultConnection 如果没有指定的{@code WebRequestMatcher}实例匹配, 则使用默认的WebConnection; never {@code null}
	 * 
	 * @return 一个新的{@code WebConnection}, 如果其中一个指定的{@code WebRequestMatcher}匹配, 它将使用{@code MockMvc}实例
	 * @deprecated Use {@link #createConnection(WebClient)} instead
	 */
	@Deprecated
	protected final WebConnection createConnection(WebConnection defaultConnection) {
		Assert.notNull(defaultConnection, "Default WebConnection must not be null");
		return createConnection(new WebClient(), defaultConnection);
	}

	/**
	 * 如果指定的{@link WebRequestMatcher}实例之一匹配, 则将创建使用{@link MockMvc}实例的新{@link WebConnection}.
	 * 
	 * @param webClient 如果没有指定的{@code WebRequestMatcher}实例匹配, 使用的WebClient (never {@code null})
	 * 
	 * @return 一个新的{@code WebConnection}, 如果其中一个指定的{@code WebRequestMatcher}匹配, 它将使用{@code MockMvc}实例
	 */
	protected final WebConnection createConnection(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		return createConnection(webClient, webClient.getWebConnection());
	}

	private WebConnection createConnection(WebClient webClient, WebConnection defaultConnection) {
		WebConnection connection = new MockMvcWebConnection(this.mockMvc, webClient, this.contextPath);
		if (this.alwaysUseMockMvc) {
			return connection;
		}
		List<DelegateWebConnection> delegates = new ArrayList<DelegateWebConnection>(this.requestMatchers.size());
		for (WebRequestMatcher matcher : this.requestMatchers) {
			delegates.add(new DelegateWebConnection(matcher, connection));
		}
		return new DelegatingWebConnection(defaultConnection, delegates);
	}

}
