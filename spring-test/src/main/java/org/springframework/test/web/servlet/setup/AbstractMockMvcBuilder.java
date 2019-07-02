package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilderSupport;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.ConfigurableSmartRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link org.springframework.test.web.servlet.MockMvcBuilder}的抽象实现,
 * 包含用于配置过滤器, 默认请求属性, 全局期望和全局结果操作的常用方法.
 * <p>
 * 子类可以使用不同的策略来准备WebApplicationContext以传递给DispatcherServlet.
 */
public abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
		extends MockMvcBuilderSupport implements ConfigurableMockMvcBuilder<B> {

	private List<Filter> filters = new ArrayList<Filter>();

	private RequestBuilder defaultRequestBuilder;

	private final List<ResultMatcher> globalResultMatchers = new ArrayList<ResultMatcher>();

	private final List<ResultHandler> globalResultHandlers = new ArrayList<ResultHandler>();

	private final List<DispatcherServletCustomizer> dispatcherServletCustomizers = new ArrayList<DispatcherServletCustomizer>();

	private final List<MockMvcConfigurer> configurers = new ArrayList<MockMvcConfigurer>(4);


	@SuppressWarnings("unchecked")
	public final <T extends B> T addFilters(Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");

		for (Filter f : filters) {
			Assert.notNull(f, "filters cannot contain null values");
			this.filters.add(f);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T addFilter(Filter filter, String... urlPatterns) {

		Assert.notNull(filter, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");

		if (urlPatterns.length > 0) {
			filter = new PatternMappingFilterProxy(filter, urlPatterns);
		}

		this.filters.add(filter);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
		this.globalResultMatchers.add(resultMatcher);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T alwaysDo(ResultHandler resultHandler) {
		this.globalResultHandlers.add(resultHandler);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
		this.dispatcherServletCustomizers.add(customizer);
		return (T) this;
	}

	public final <T extends B> T dispatchOptions(final boolean dispatchOptions) {
		return addDispatcherServletCustomizer(new DispatcherServletCustomizer() {
			@Override
			public void customize(DispatcherServlet dispatcherServlet) {
				dispatcherServlet.setDispatchOptionsRequest(dispatchOptions);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public final <T extends B> T apply(MockMvcConfigurer configurer) {
		configurer.afterConfigurerAdded(this);
		this.configurers.add(configurer);
		return (T) this;
	}


	/**
	 * 构建一个{@link org.springframework.test.web.servlet.MockMvc}实例.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public final MockMvc build() {

		WebApplicationContext wac = initWebAppContext();

		ServletContext servletContext = wac.getServletContext();
		MockServletConfig mockServletConfig = new MockServletConfig(servletContext);

		for (MockMvcConfigurer configurer : this.configurers) {
			RequestPostProcessor processor = configurer.beforeMockMvcCreated(this, wac);
			if (processor != null) {
				if (this.defaultRequestBuilder == null) {
					this.defaultRequestBuilder = MockMvcRequestBuilders.get("/");
				}
				if (this.defaultRequestBuilder instanceof ConfigurableSmartRequestBuilder) {
					((ConfigurableSmartRequestBuilder) this.defaultRequestBuilder).with(processor);
				}
			}
		}

		Filter[] filterArray = this.filters.toArray(new Filter[this.filters.size()]);

		return super.createMockMvc(filterArray, mockServletConfig, wac, this.defaultRequestBuilder,
				this.globalResultMatchers, this.globalResultHandlers, this.dispatcherServletCustomizers);
	}

	/**
	 * 获取要传递给DispatcherServlet的WebApplicationContext的方法.
	 * 在创建{@link org.springframework.test.web.servlet.MockMvc}实例之前从{@link #build()}调用.
	 */
	protected abstract WebApplicationContext initWebAppContext();

}