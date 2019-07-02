package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code CacheAwareContextLoaderDelegate}负责{@linkplain #loadContext loading}
 * 和{@linkplain #closeContext closing}应用程序上下文, 与幕后的
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * 透明地交互.
 *
 * <p>Note: {@code CacheAwareContextLoaderDelegate}不扩展{@link ContextLoader} 或 {@link SmartContextLoader}接口.
 */
public interface CacheAwareContextLoaderDelegate {

	/**
	 * 通过委托给{@code MergedContextConfiguration}中配置的{@link ContextLoader},
	 * 为所提供的{@link MergedContextConfiguration}加载{@linkplain ApplicationContext 应用程序上下文}.
	 * <p>如果{@code ContextCache}中存在上下文, 返回; 否则, 它将被加载, 存储在缓存中并返回.
	 * <p>应通过调用
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()}来记录缓存统计信息.
	 * 
	 * @param mergedContextConfiguration 要加载的应用程序上下文的合并上下文配置; never {@code null}
	 * 
	 * @return 应用程序上下文
	 * @throws IllegalStateException 如果在检索或加载应用程序上下文时发生错误
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration);

	/**
	 * 如果它是{@link ConfigurableApplicationContext}的实例,
	 * 则从{@code ContextCache}和{@linkplain ConfigurableApplicationContext#close() close}中
	 * 删除所提供的{@link MergedContextConfiguration}的{@linkplain ApplicationContext 应用程序上下文}.
	 * <p>从缓存中删除上下文时, 必须遵守所提供的{@code HierarchyMode}的语义.
	 * 有关详细信息, 请参阅{@link HierarchyMode}的Javadoc.
	 * <p>一般来说, 只有在单个bean的状态发生变化 (可能影响将来与上下文的交互),
	 * 或者需要过早地从缓存中删除上下文时才应调用此方法.
	 * 
	 * @param mergedContextConfiguration 要关闭的应用程序上下文的合并上下文配置; never {@code null}
	 * @param hierarchyMode 层次模式; 如果上下文不是层次结构的一部分, 则可能是{@code null}
	 */
	void closeContext(MergedContextConfiguration mergedContextConfiguration, HierarchyMode hierarchyMode);

}
