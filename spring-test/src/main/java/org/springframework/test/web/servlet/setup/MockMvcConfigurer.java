package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;


/**
 * Allows a sub-class to encapsulate logic for pre-configuring a
 * {@code ConfigurableMockMvcBuilder} for some specific purpose. A 3rd party
 * library may use this to provide shortcuts for setting up MockMvc.
 *
 * <p>Can be plugged in via {@link ConfigurableMockMvcBuilder#apply} with
 * instances of this type likely created via static methods, e.g.:
 *
 * <pre class="code">
 * 	MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 */
public interface MockMvcConfigurer {

	/**
	 * Invoked immediately after a {@code MockMvcConfigurer} is added via
	 * {@link ConfigurableMockMvcBuilder#apply}.
	 */
	void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder);

	/**
	 * Invoked just before the MockMvc instance is created. Implementations may
	 * return a RequestPostProcessor to be applied to every request performed
	 * through the created {@code MockMvc} instance.
	 */
	RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context);

}
