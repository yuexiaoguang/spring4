package org.springframework.test.context;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessor;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code TestContext}封装了执行测试的上下文, 与使用中的实际测试框架无关.
 */
public interface TestContext extends AttributeAccessor, Serializable {

	/**
	 * 获取此测试上下文的{@linkplain ApplicationContext 应用程序上下文}, 可能已缓存.
	 * <p>如果尚未加载相应的上下文, 则此方法的实现负责加载应用程序上下文, 也可能缓存上下文.
	 * 
	 * @return 应用程序上下文
	 * @throws IllegalStateException 如果在检索应用程序上下文时发生错误
	 */
	ApplicationContext getApplicationContext();

	/**
	 * 获取此测试上下文的{@linkplain Class 测试类}.
	 * 
	 * @return 测试类 (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * 获取此测试上下文的当前{@linkplain Object 测试实例}.
	 * <p>Note: 这是一个可变的属性.
	 * 
	 * @return 当前的测试实例 (may be {@code null})
	 */
	Object getTestInstance();

	/**
	 * 获取此测试上下文的当前{@linkplain Method 测试方法}.
	 * <p>Note: 这是一个可变的属性.
	 * 
	 * @return 当前的测试方法 (may be {@code null})
	 */
	Method getTestMethod();

	/**
	 * 获取在执行{@linkplain #getTestMethod() 测试方法}期间抛出的{@linkplain Throwable exception}.
	 * <p>Note: 这是一个可变的属性.
	 * 
	 * @return 抛出的异常, 或{@code null}如果没有抛出异常
	 */
	Throwable getTestException();

	/**
	 * 调用此方法以表示与此测试上下文关联的 {@linkplain ApplicationContext 应用程序上下文}是<em>dirty</em>,
	 * 并应从上下文缓存中删除.
	 * <p>如果测试修改了上下文, 请执行此操作&mdash; 例如, 通过修改单例bean的状态, 修改嵌入式数据库的状态等.
	 * 
	 * @param hierarchyMode 如果上下文是层次结构的一部分, 则应用上下文缓存清除模式 (may be {@code null})
	 */
	void markApplicationContextDirty(HierarchyMode hierarchyMode);

	/**
	 * 更新此测试上下文以反映当前正在执行的测试的状态.
	 * <p>Caution: 此方法的并发调用可能不是线程安全的, 具体取决于底层实现.
	 * 
	 * @param testInstance 当前的测试实例 (may be {@code null})
	 * @param testMethod 当前的测试方法 (may be {@code null})
	 * @param testException 测试方法中抛出的异常, 或{@code null}如果没有抛出异常
	 */
	void updateState(Object testInstance, Method testMethod, Throwable testException);

}
