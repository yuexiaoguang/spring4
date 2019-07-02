package org.springframework.test.web.servlet.request;

import org.springframework.test.web.servlet.SmartRequestBuilder;


/**
 * 可以使用{@link RequestPostProcessor}配置的
 * {@link org.springframework.test.web.servlet.SmartRequestBuilder SmartRequestBuilder}的扩展.
 */
public interface ConfigurableSmartRequestBuilder<B extends ConfigurableSmartRequestBuilder<B>>
		extends SmartRequestBuilder {

	/**
	 * 添加给定的{@code RequestPostProcessor}.
	 */
	B with(RequestPostProcessor requestPostProcessor);

}
