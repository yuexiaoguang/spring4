package org.springframework.test.web.servlet.setup;

import javax.servlet.Filter;

import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 定义构建{@code MockMvc}的常用方法.
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {

	/**
	 * 添加映射到任何请求的过滤器 (i.e. "/*"). 例如:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(springSecurityFilterChain);
	 * </pre>
	 *
	 * <p>相当于以下web.xml配置:
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>过滤器将按照提供的顺序调用.
	 *
	 * @param filters 要添加的过滤器
	 */
	<T extends B> T addFilters(Filter... filters);

	/**
	 * 添加映射到特定模式集合的过滤器. 例如:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(myResourceFilter, "/resources/*");
	 * </pre>
	 *
	 * <p>等同于:
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;myResourceFilter&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>过滤器将按照提供的顺序调用.
	 *
	 * @param filter 要添加的过滤器
	 * @param urlPatterns 要映射到的URL模式; 如果为空, 则默认使用"/*"
	 */
	<T extends B> T addFilter(Filter filter, String... urlPatterns);

	/**
	 * 定义应合并到所有已执行请求中的默认请求属性.
	 * 实际上, 这提供了一种机制, 用于为所有请求定义公共​​初始化, 例如内容类型, 请求参数, 会话属性和任何其他请求属性.
	 *
	 * <p>执行请求时指定的属性会覆盖此处定义的默认属性.
	 *
	 * @param requestBuilder RequestBuilder; 请参阅
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}中的静态工厂方法
	 * .
	 */
	<T extends B> T defaultRequest(RequestBuilder requestBuilder);

	/**
	 * 定义应<em>始终</em>应用于每个响应的全局期望.
	 * 例如, 状态码 200 (OK), 内容类型{@code "application/json"}, etc.
	 *
	 * @param resultMatcher ResultMatcher; 请参阅
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}中的静态工厂方法
	 */
	<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

	/**
	 * 定义应<em>始终</em>应用于每个响应的全局操作.
	 * 例如, 将有关执行的请求和生成的响应的详细信息写入{@code System.out}.
	 *
	 * @param resultHandler ResultHandler; 请参阅
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}中的静态工厂方法
	 */
	<T extends B> T alwaysDo(ResultHandler resultHandler);

	/**
	 * 是否启用DispatcherServlet属性
	 * {@link org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest dispatchOptionsRequest},
	 * 该属性允许处理HTTP OPTIONS请求.
	 */
	<T extends B> T dispatchOptions(boolean dispatchOptions);

	/**
	 * 添加一个{@code MockMvcConfigurer}, 可以自动执行MockMvc设置并为特定目的配置它 (e.g. 安全).
	 */
	<T extends B> T apply(MockMvcConfigurer configurer);

}
