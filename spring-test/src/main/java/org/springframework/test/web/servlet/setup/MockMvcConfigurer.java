package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;


/**
 * 允许子类封装逻辑, 以便为某些特定目的预先配置{@code ConfigurableMockMvcBuilder}.
 * 第三方库可以使用它来提供设置MockMvc的快捷方式.
 *
 * <p>可以通过{@link ConfigurableMockMvcBuilder#apply}插入, 可能通过静态方法创建此类型的实例, e.g.:
 *
 * <pre class="code">
 * 	MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 */
public interface MockMvcConfigurer {

	/**
	 * 通过{@link ConfigurableMockMvcBuilder#apply}添加{@code MockMvcConfigurer}后立即调用.
	 */
	void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder);

	/**
	 * 在创建MockMvc实例之前调用.
	 * 实现可以返回RequestPostProcessor, 以应用于通过创建的{@code MockMvc}实例执行的每个请求.
	 */
	RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context);

}
