package org.springframework.test.web.servlet;

/**
 * A {@code ResultHandler} performs a generic action on the result of an
 * executed request &mdash; for example, printing debug information.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers
 * MockMvcResultHandlers}.
 *
 * <h3>Example</h3>
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
	 * Perform an action on the given result.
	 *
	 * @param result the result of the executed request
	 * @throws Exception if a failure occurs
	 */
	void handle(MvcResult result) throws Exception;

}
