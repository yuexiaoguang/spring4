package org.springframework.test.web.servlet;

/**
 * Allows applying actions, such as expectations, on the result of an executed
 * request.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers} and
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}.
 */
public interface ResultActions {

	/**
	 * Perform an expectation.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/person/1"))
	 *   .andExpect(status().isOk())
	 *   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 *   .andExpect(jsonPath("$.person.name").value("Jason"));
	 *
	 * mockMvc.perform(post("/form"))
	 *   .andExpect(status().isOk())
	 *   .andExpect(redirectedUrl("/person/1"))
	 *   .andExpect(model().size(1))
	 *   .andExpect(model().attributeExists("person"))
	 *   .andExpect(flash().attributeCount(1))
	 *   .andExpect(flash().attribute("message", "success!"));
	 * </pre>
	 */
	ResultActions andExpect(ResultMatcher matcher) throws Exception;

	/**
	 * Perform a general action.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/form")).andDo(print());
	 * </pre>
	 */
	ResultActions andDo(ResultHandler handler) throws Exception;

	/**
	 * Return the result of the executed request for direct access to the results.
	 *
	 * @return the result of the request
	 */
	MvcResult andReturn();

}
