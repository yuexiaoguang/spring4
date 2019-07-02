package org.springframework.test.web.servlet;

/**
 * 允许对执行的请求的结果应用操作, 例如期望.
 *
 * <p>请参阅
 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}
 * 和{@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}中的静态工厂方法.
 */
public interface ResultActions {

	/**
	 * 执行预期.
	 *
	 * <h4>示例</h4>
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
	 * 执行一般操作.
	 *
	 * <h4>示例</h4>
	 * <pre class="code">
	 * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/form")).andDo(print());
	 * </pre>
	 */
	ResultActions andDo(ResultHandler handler) throws Exception;

	/**
	 * 返回执行请求的结果, 以直接访问结果.
	 *
	 * @return 请求的结果
	 */
	MvcResult andReturn();

}
