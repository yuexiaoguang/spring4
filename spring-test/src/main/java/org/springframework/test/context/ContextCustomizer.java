package org.springframework.test.context;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * 用于自定义由<em>Spring TestContext Framework</em>创建和管理的
 * {@link ConfigurableApplicationContext 应用程序上下文}的策略接口.
 *
 * <p>由{@link ContextCustomizerFactory}实现创建的自定义器.
 *
 * <p>实现必须实现正确的{@code equals}和{@code hashCode}方法,
 * 因为定制器构成{@link MergedContextConfiguration}的一部分, 用作缓存的键.
 */
public interface ContextCustomizer {

	/**
	 * 在将bean定义加载到上下文<em>之后</em>, 但在上下文刷新<em>之前</em>,
	 * 自定义提供的{@code ConfigurableApplicationContext}.
	 * 
	 * @param context 要定制的上下文
	 * @param mergedConfig 合并的上下文配置
	 */
	void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
