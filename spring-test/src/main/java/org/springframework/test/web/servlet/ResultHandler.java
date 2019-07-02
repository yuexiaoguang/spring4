package org.springframework.test.web.servlet;

/**
 * {@code ResultHandler}对执行的请求的结果执行一般操作 &mdash; 例如, 打印调试信息.
 *
 * <p>请参阅
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers MockMvcResultHandlers}中的静态工厂方法.
 *
 * <h3>示例</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form")).andDo(print());
 * </pre>
 */
public interface ResultHandler {

	/**
	 * 对给定结果执行操作.
	 *
	 * @param result 执行请求的结果
	 * 
	 * @throws Exception 如果发生错误
	 */
	void handle(MvcResult result) throws Exception;

}
