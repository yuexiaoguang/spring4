package org.springframework.test.web.servlet;

/**
 * 构建一个{@link MockMvc}实例.
 *
 * <p>请参阅
 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders MockMvcBuilders}中的静态工厂方法.
 */
public interface MockMvcBuilder {

	/**
	 * 构建一个{@link MockMvc}实例.
	 */
	MockMvc build();

}