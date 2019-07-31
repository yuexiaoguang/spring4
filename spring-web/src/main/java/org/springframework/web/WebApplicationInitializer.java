package org.springframework.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 要在Servlet 3.0+环境中实现的接口, 以便以编程方式配置{@link ServletContext}
 * -- 与传统的基于{@code web.xml}的传统方法相反(或可能与此相结合).
 *
 * <p>{@link SpringServletContainerInitializer}将自动检测此SPI的实现, 它本身由任何Servlet 3.0容器自动引导.
 * 有关此引导机制的详细信息, 请参阅{@linkplain SpringServletContainerInitializer its Javadoc}.
 *
 * <h2>Example</h2>
 * <h3>传统的基于XML的方法</h3>
 * 构建Web应用程序的大多数Spring用户需要注册Spring的{@code DispatcherServlet}.
 * 作为参考, 在WEB-INF/web.xml中, 这通常如下进行:
 * <pre class="code">
 * {@code
 * <servlet>
 *   <servlet-name>dispatcher</servlet-name>
 *   <servlet-class>
 *     org.springframework.web.servlet.DispatcherServlet
 *   </servlet-class>
 *   <init-param>
 *     <param-name>contextConfigLocation</param-name>
 *     <param-value>/WEB-INF/spring/dispatcher-config.xml</param-value>
 *   </init-param>
 *   <load-on-startup>1</load-on-startup>
 * </servlet>
 *
 * <servlet-mapping>
 *   <servlet-name>dispatcher</servlet-name>
 *   <url-pattern>/</url-pattern>
 * </servlet-mapping>}</pre>
 *
 * <h3>{@code WebApplicationInitializer}基于代码的方法</h3>
 * 这是等效的{@code DispatcherServlet}注册逻辑, {@code WebApplicationInitializer}-style:
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述的替代方法, 还可以从
 * {@link org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}扩展.
 *
 * 正如您所看到的, 感谢Servlet 3.0的新{@link ServletContext#addServlet}方法,
 * 我们实际上正在注册{@code DispatcherServlet}的<em>实例</em>,
 * 这意味着{@code DispatcherServlet}现在可以像任何其他对象一样对待 -- 在这种情况下接收构造函数注入其应用程序上下文.
 *
 * <p>这种风格既简单又简洁.
 * 处理init-params等无关紧要, 只需要普通的JavaBean样式属性和构造函数参数.
 * 在将它们注入{@code DispatcherServlet}之前, 可以根据需要自由创建和使用Spring应用程序上下文.
 *
 * <p>大多数主要的Spring Web组件都已更新, 以支持这种注册方式.
 * 你会发现{@code DispatcherServlet}, {@code FrameworkServlet}, {@code ContextLoaderListener}
 * 和{@code DelegatingFilterProxy}现在都支持构造函数参数.
 * 即使某个组件 (e.g. 非Spring, 其他第三方) 未在{@code WebApplicationInitializers}中进行专门更新, 它们仍然可以在任何情况下使用.
 * Servlet 3.0 {@code ServletContext} API允许以编程方式设置init-params, context-params等.
 *
 * <h2>100% 基于代码的配置方法</h2>
 * 在上面的示例中, {@code WEB-INF/web.xml}已成功替换为{@code WebApplicationInitializer}形式的代码,
 * 但实际的{@code dispatcher-config.xml} Spring配置仍然基于XML.
 * {@code WebApplicationInitializer}非常适合与Spring基于代码的{@code @Configuration}类一起使用.
 * 有关完整的详细信息, 请参阅@{@link org.springframework.context.annotation.Configuration Configuration} Javadoc,
 * 但以下示例演示了使用Spring的
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}
 * 代替{@code XmlWebApplicationContext},
 * 以及用户定义的{@code @Configuration}, {@code AppConfig} 和 {@code DispatcherConfig}代替Spring XML文件.
 * 这个例子也有点超出上面演示的那些'root'应用程序上下文的典型配置和{@code ContextLoaderListener}的注册:
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // Create the 'root' Spring application context
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // Manage the lifecycle of the root application context
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // Create the dispatcher servlet's Spring application context
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // Register and map the dispatcher servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述的替代方法, 还可以从
 * {@link org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}扩展.
 *
 * 请记住{@code WebApplicationInitializer}实现是<em>自动检测</em> -- 因此可以根据需要将它们打包到应用程序中.
 *
 * <h2>{@code WebApplicationInitializer}执行顺序</h2>
 * {@code WebApplicationInitializer}实现可以选择
 * 使用Spring的 @{@link org.springframework.core.annotation.Order Order}注解在类级别进行注解,
 * 也可以实现Spring的{@link org.springframework.core.Ordered Ordered}接口.
 * 如果是这样, 将在调用之前排序初始化器.
 * 这为用户提供了一种机制, 以确保servlet容器初始化的顺序.
 * 预计此功能的使用很少, 因为典型的应用程序可能会将所有容器初始化集中在一个{@code WebApplicationInitializer}中.
 *
 * <h2>注意事项</h2>
 *
 * <h3>web.xml 版本</h3>
 * <p>{@code WEB-INF/web.xml}和{@code WebApplicationInitializer}使用不是互斥的;
 * 例如, web.xml可以注册一个servlet, 而{@code WebApplicationInitializer}可以注册另一个servlet.
 * 初始化器甚至可以通过{@link ServletContext#getServletRegistration(String)}等方法<em>修改</em>在{@code web.xml}中执行的注册.
 * <strong>但是, 如果应用程序中存在{@code WEB-INF/web.xml}, 则其{@code version}属性必须设置为"3.0"或更高,
 * 否则servlet容器将忽略{@code ServletContainerInitializer}引导.</strong>
 *
 * <h3>在Tomcat下映射到'/'</h3>
 * <p>Apache Tomcat将其内部{@code DefaultServlet}映射到 "/", 而在Tomcat版本 &lt;= 7.0.14上,
 * <em>无法以编程方式覆盖此servlet映射</em>. 7.0.15修复了这个问题. 覆盖"/" servlet映射也已在GlassFish 3.1下成功测试.<p>
 */
public interface WebApplicationInitializer {

	/**
	 * 使用初始化此Web应用程序所需的任何servlet, 过滤器, 监听器context-params和属性配置给定的{@link ServletContext}.
	 * See examples {@linkplain WebApplicationInitializer above}.
	 * 
	 * @param servletContext 要初始化的{@code ServletContext}
	 * 
	 * @throws ServletException 如果调用给定{@code ServletContext}时抛出{@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
