package org.springframework.web.util;

import java.beans.Introspector;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.CachedIntrospectionResults;

/**
 * 监听器, 在Web应用程序关闭时刷新JDK的{@link java.beans.Introspector JavaBeans Introspector}缓存.
 * 在{@code web.xml}中注册此监听器, 以确保正确释放Web应用程序类加载器及其加载的类.
 *
 * <p><b>如果已使用JavaBeans Introspector分析应用程序类, 则系统级的Introspector缓存将保留对这些类的硬引用.
 * 因此, 这些类和Web应用程序类加载器将不会在Web应用程序关闭时进行垃圾收集!</b>
 * 此监听器执行适当的清理, 以允许垃圾收集生效.
 *
 * <p>不幸的是, 清理Introspector的唯一方法是刷新整个缓存, 因为没有办法专门确定那里引用的应用程序的类.
 * 这将删除服务器中所有其他应用程序缓存的内省结果.
 *
 * <p>请注意, 在应用程序中使用Spring的bean基础结构时, 此监听器<i>不</i>是必要的,
 * 因为Spring自己的内省结果缓存将立即从JavaBeans Introspector缓存中刷新分析的类,
 * 并且只在应用程序自己的ClassLoader中保存缓存.
 *
 * <b>虽然Spring本身不会创建JDK Introspector泄漏, 但请注意,
 * 在Spring框架类本身驻留在'公共' ClassLoader (例如系统ClassLoader)中的场景中, 仍然应该使用这个监听器.</b>
 * 在这种情况下, 此监听器将正确清理Spring的内省缓存.
 *
 * <p>应用程序类几乎不需要直接使用JavaBeans Introspector, 因此通常不会导致Introspector资源泄漏.
 * 相反, 许多库和框架不会清理Introspector: e.g. Struts 和 Quartz.
 *
 * <p>请注意, 单个此类Introspector泄漏将导致整个Web应用程序类加载器无法收集垃圾!
 * 这样做的结果是, 在Web应用程序关闭后, 将看到所有应用程序的静态类资源 (如单例), 这不是这些类的错误!
 *
 * <p><b>在任何应用程序监听器(如Spring的ContextLoaderListener)之前,
 * 应将此监听器注册为{@code web.xml}中的第一个监听器.</b>
 * 这允许监听器在生命周期的正确时间充分发挥作用.
 */
public class IntrospectorCleanupListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
		Introspector.flushCaches();
	}

}
