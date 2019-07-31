package org.springframework.web;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Servlet 3.0 {@link ServletContainerInitializer},
 * 旨在使用Spring的{@link WebApplicationInitializer} SPI支持基于代码的servlet容器配置,
 * 而不是传统的基于{@code web.xml}的方法(或可能与之结合使用).
 *
 * <h2>运作机制</h2>
 * 该类将被加载并实例化，并在容器启动期间由任何符合Servlet 3.0的容器调用其{@link #onStartup}方法,
 * 假设类路径上存在{@code spring-web}模块JAR.
 * 这是通过检测{@code spring-web}模块的{@code META-INF/services/javax.servlet.ServletContainerInitializer}
 * 服务提供者配置文件的JAR服务API {@link ServiceLoader#load(Class)}方法实现的.
 * See the
 * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR Services API documentation</a> as well as section <em>8.2.4</em> of the Servlet 3.0 Final Draft specification for complete details.
 *
 * <h3>与{@code web.xml}结合使用</h3>
 * Web应用程序可以选择通过{@code web.xml}中的{@code metadata-complete}属性来限制Servlet容器在启动时执行的类路径扫描数量,
 * 该属性控制着对Servlet注解的扫描, 或者通过{@code web.xml}中的{@code <absolute-ordering>}元素进行控制,
 * 该元素控制允许哪些Web片段(i.e. jars)执行{@code ServletContainerInitializer}扫描.
 * 使用此功能时, 可以通过将"spring_web"添加到{@code web.xml}中的命名Web片段列表
 * 来启用{@link SpringServletContainerInitializer}, 如下所示:
 *
 * <pre class="code">
 * {@code
 * <absolute-ordering>
 *   <name>some_web_fragment</name>
 *   <name>spring_web</name>
 * </absolute-ordering>
 * }</pre>
 *
 * <h2>与Spring的{@code WebApplicationInitializer}的关系</h2>
 * Spring的{@code WebApplicationInitializer} SPI只包含一个方法:
 * {@link WebApplicationInitializer#onStartup(ServletContext)}.
 * 签名有意与{@link ServletContainerInitializer#onStartup(Set, ServletContext)}非常相似:
 * 简而言之, {@code SpringServletContainerInitializer}负责实例化{@code ServletContext}
 * 并将其委托给任何用户定义的{@code WebApplicationInitializer}实现.
 * 然后由每个{@code WebApplicationInitializer}负责初始化{@code ServletContext}的实际工作.
 * 委托的确切过程在下面的{@link #onStartup onStartup}文档中有详细描述.
 *
 * <h2>一般注意事项</h2>
 * 通常, 对于更重要且面向用户的{@code WebApplicationInitializer} SPI, 此类应被视为<em>支持基础架构</em>.
 * 利用此容器初始化器也完全<em>可选</em>:
 * 虽然这个初始化器确实会在所有Servlet 3.0+运行时下加载和调用, 但用户仍然可以选择是否在类路径上提供{@code WebApplicationInitializer}实现.
 * 如果未检测到{@code WebApplicationInitializer}类型, 则此容器初始化器将不起作用.
 *
 * <p>请注意, 使用此容器初始化器和{@code WebApplicationInitializer}除了在{@code spring-web}模块JAR中提供类型的事实之外,
 * 不会以任何方式"绑定"到Spring MVC.
 * 相反, 它们可以被认为是通用的, 有助于基于代码的{@code ServletContext}配置.
 * 换句话说, 任何servlet, 监听器或过滤器都可以在{@code WebApplicationInitializer}中注册, 而不仅仅是Spring MVC特定的组件.
 *
 * <p>此类既不是为扩展而设计的, 也不是为了扩展.
 * 它应被视为内部类型, {@code WebApplicationInitializer}是面向公众的SPI.
 *
 * <h2>See Also</h2>
 * 有关示例和详细用法建议, 请参阅{@link WebApplicationInitializer} Javadoc.<p>
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * 将{@code ServletContext}委托给应用程序类路径上的任何{@link WebApplicationInitializer}实现.
	 * <p>因为这个类声明 @{@code HandlesTypes(WebApplicationInitializer.class)},
	 * 所以Servlet 3.0+容器会自动扫描类路径以获取Spring的{@code WebApplicationInitializer}接口的实现,
	 * 并将所有这些类型的集合提供给此方法的{@code webAppInitializerClasses}参数.
	 * <p>如果在类路径上找不到{@code WebApplicationInitializer}实现, 则此方法实际上是无操作.
	 * 将发出INFO级别的日志消息, 通知用户确实已调用{@code ServletContainerInitializer},
	 * 但未找到{@code WebApplicationInitializer}实现.
	 * <p>假设检测到一个或多个{@code WebApplicationInitializer}类型, 它们将被实例化
	 * (并<em>排序</em>, 如果存在@{@link org.springframework.core.annotation.Order @Order}注解,
	 * 或实现了{@link org.springframework.core.Ordered Ordered}接口).
	 * 然后将在每个实例上调用{@link WebApplicationInitializer#onStartup(ServletContext)}方法,
	 * 委托{@code ServletContext}使每个实例可以注册和配置Servlet (例如Spring的{@code DispatcherServlet}),
	 * 监听器(如Spring的{@code ContextLoaderListener}), 或任何其他Servlet API组件(如过滤器).
	 * 
	 * @param webAppInitializerClasses 在应用程序类路径中找到的{@link WebApplicationInitializer}的所有实现
	 * @param servletContext 要初始化的servlet上下文
	 */
	@Override
	public void onStartup(Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		List<WebApplicationInitializer> initializers = new LinkedList<WebApplicationInitializer>();

		if (webAppInitializerClasses != null) {
			for (Class<?> waiClass : webAppInitializerClasses) {
				// 要小心: 无论@HandlesTypes说什么, 有些servlet容器会提供无效的类...
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						initializers.add((WebApplicationInitializer) waiClass.newInstance());
					}
					catch (Throwable ex) {
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
		AnnotationAwareOrderComparator.sort(initializers);
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
