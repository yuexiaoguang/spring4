package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

/**
 * 要导入的主要类, 以便访问所有可用的{@link MockMvcBuilder}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to this setting, open the Preferences and type "favorites".
 */
public class MockMvcBuilders {

	/**
	 * 使用给定的完全初始化的(i.e., <em>刷新</em>){@link WebApplicationContext}构建{@link MockMvc}实例.
	 * <p>{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * 将使用上下文在其中发现Spring MVC基础结构和应用程序控制器.
	 * 必须使用{@link javax.servlet.ServletContext ServletContext}配置上下文.
	 */
	public static DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context) {
		return new DefaultMockMvcBuilder(context);
	}

	/**
	 * 通过注册一个或多个{@code @Controller}实例并以编程方式配置Spring MVC基础结构来构建{@link MockMvc}实例.
	 * <p>这允许完全控制控制器及其依赖关系的实例化和初始化, 类似于普通单元测试, 同时还可以一次测试一个控制器.
	 * <p>使用此构建器时, {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * 为带有带注解的控制器的请求提供服务所需的最小基础结构是自动创建的并且可以自定义,
	 * 除了使用构建器样式的方法之外, 该配置等同于MVC Java配置提供的配置.
	 * <p>如果应用程序的Spring MVC配置相对简单 &mdash; 例如, 在XML或MVC Java配置中使用MVC命名空间时 &mdash;
	 * 那么使用这个构建器可能是测试大多数控制器的一个很好的选择.
	 * 在这种情况下, 可以使用少得多的测试来专注于测试和验证实际的Spring MVC配置.
	 * 
	 * @param controllers 要测试的一个或多个{@code @Controller}实例
	 */
	public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
		return new StandaloneMockMvcBuilder(controllers);
	}

}
