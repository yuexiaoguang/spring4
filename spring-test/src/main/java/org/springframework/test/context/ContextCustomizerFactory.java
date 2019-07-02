package org.springframework.test.context;

import java.util.List;

/**
 * 创建{@link ContextCustomizer ContextCustomizers}的工厂.
 *
 * <p>在{@link ContextLoader ContextLoaders}处理了上下文配置属性之后,
 * 但在创建{@link MergedContextConfiguration}之前调用工厂.
 *
 * <p>默认情况下, Spring TestContext Framework将使用
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * 机制来加载在类路径上的所有{@code META-INF/spring.factories}文件中配置的工厂.
 */
public interface ContextCustomizerFactory {

	/**
	 * 创建一个{@link ContextCustomizer}, 用于在刷新
	 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
	 * 之前自定义它.
	 * 
	 * @param testClass 测试类
	 * @param configAttributes 测试类的上下文配置属性列表, 有序<em>自下而上</em> (i.e., 好像正在遍历类层次结构); never {@code null} or empty
	 * 
	 * @return {@link ContextCustomizer}, 或{@code null}如果不应使用定制器
	 */
	ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
