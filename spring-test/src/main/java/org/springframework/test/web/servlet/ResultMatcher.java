package org.springframework.test.web.servlet;

/**
 * {@code ResultMatcher}将执行的请求的结果与某些期望相匹配.
 *
 * <p>请参阅
 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers MockMvcResultMatchers}中的静态工厂方法.
 *
 * <h3>使用状态和内容结果匹配器的示例</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *   .andExpect(status().isOk())
 *   .andExpect(content().mimeType(MediaType.APPLICATION_JSON));
 * </pre>
 */
public interface ResultMatcher {

	/**
	 * 断言执行请求的结果.
	 * 
	 * @param result 执行请求的结果
	 * 
	 * @throws Exception 如果发生错误
	 */
	void match(MvcResult result) throws Exception;

}
