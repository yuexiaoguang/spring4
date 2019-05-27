package org.springframework.test.web.servlet;

/**
 * Builds a {@link MockMvc} instance.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders MockMvcBuilders}.
 */
public interface MockMvcBuilder {

	/**
	 * Build a {@link MockMvc} instance.
	 */
	MockMvc build();

}