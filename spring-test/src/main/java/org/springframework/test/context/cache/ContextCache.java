package org.springframework.test.context.cache;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@code ContextCache}定义用于在<em>Spring TestContext Framework</em>中
 * 缓存Spring {@link ApplicationContext ApplicationContexts}.
 *
 * <p>{@code ContextCache}维护{@code ApplicationContexts}的缓存,
 * 该缓存由{@link MergedContextConfiguration}实例作为键,
 * 可能配置有{@linkplain ContextCacheUtils#retrieveMaxCacheSize 最大大小}和自定义驱逐策略.
 *
 * <h3>原因</h3>
 * <p>如果上下文初始化很复杂, 上下文缓存可以带来显着的性能优势.
 * 虽然Spring上下文本身的初始化通常非常快, 但是上下文中的一些bean &mdash;
 * 例如, 嵌入式数据库或{@code LocalContainerEntityManagerFactoryBean}用于处理JPA &mdash; 初始化可能需要几秒钟.
 * 因此, 每个测试套件或JVM进程只执行一次初始化通常是有意义的.
 */
public interface ContextCache {

	/**
	 * 用于报告{@code ContextCache}统计信息的日志记录类别的名称.
	 */
	String CONTEXT_CACHE_LOGGING_CATEGORY = "org.springframework.test.context.cache";

	/**
	 * 上下文缓存的默认最大大小: {@value}.
	 */
	int DEFAULT_MAX_CONTEXT_CACHE_SIZE = 32;

	/**
	 * 系统属性, 用于将{@link ContextCache}的最大大小配置为正整数.
	 * 也可以通过{@link org.springframework.core.SpringProperties}机制进行配置.
	 * <p>请注意, {@code ContextCache}的实现不需要实际支持最大缓存大小.
	 * 有关详细信息, 请参阅相应实现的文档.
	 */
	String MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME = "spring.test.context.cache.maxSize";


	/**
	 * 确定给定键是否存在缓存的上下文.
	 * 
	 * @param key 上下文键 (never {@code null})
	 * 
	 * @return {@code true} 如果缓存包含具有给定键的上下文
	 */
	boolean contains(MergedContextConfiguration key);

	/**
	 * 获取给定键的缓存{@code ApplicationContext}.
	 * <p>必须相应更新{@linkplain #getHitCount() hit}和{@linkplain #getMissCount() miss}计数.
	 * 
	 * @param key 上下文键 (never {@code null})
	 * 
	 * @return 相应的{@code ApplicationContext}实例, 或{@code null}如果在缓存中找不到
	 */
	ApplicationContext get(MergedContextConfiguration key);

	/**
	 * 显式将{@code ApplicationContext}实例添加到给定键下的缓存中, 可能会遵循自定义驱逐策略.
	 * 
	 * @param key 上下文键 (never {@code null})
	 * @param context {@code ApplicationContext}实例 (never {@code null})
	 */
	void put(MergedContextConfiguration key, ApplicationContext context);

	/**
	 * 从缓存中删除具有给定键的上下文, 如果它是{@code ConfigurableApplicationContext}的实例, 则显式地
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#close() close}.
	 * <p>一般来说, 应调用此方法以从缓存中正确驱逐上下文 (e.g., 由于自定义驱逐策略),
	 * 或者单个bean的状态已被修改, 可能影响将来与上下文的交互.
	 * <p>此外, 必须遵守所提供的{@code HierarchyMode}的语义. 有关详细信息, 请参阅{@link HierarchyMode} Javadoc.
	 * 
	 * @param key 上下文键; never {@code null}
	 * @param hierarchyMode 层次模式; 可能是{@code null}, 如果上下文不是层次结构的一部分
	 */
	void remove(MergedContextConfiguration key, HierarchyMode hierarchyMode);

	/**
	 * 确定当前存储在缓存中的上下文数量.
	 * <p>如果缓存包含超过{@code Integer.MAX_VALUE}个元素, 则此方法必须返回{@code Integer.MAX_VALUE}.
	 */
	int size();

	/**
	 * 确定当前在缓存中跟踪的父上下文的数量.
	 */
	int getParentContextCount();

	/**
	 * 获取此缓存的总点击次数.
	 * <p><em>hit</em>是对缓存的任何访问, 它返回查询键的非空上下文.
	 */
	int getHitCount();

	/**
	 * 获取此缓存的总体未命中数.
	 * <p><em>miss</em>是对缓存的任何访问, 为查询的键返回{@code null}上下文.
	 */
	int getMissCount();

	/**
	 * 重置此缓存维护的所有状态, 包括统计信息.
	 */
	void reset();

	/**
	 * 清除缓存中的所有上下文, 同时清除上下文层次结构信息.
	 */
	void clear();

	/**
	 * 清除缓存的命中和未命中计数统计信息 (i.e., 将计数器重置为零).
	 */
	void clearStatistics();

	/**
	 * 使用{@value #CONTEXT_CACHE_LOGGING_CATEGORY}日志记录类别在{@code DEBUG}级别记录此{@code ContextCache}的统计信息.
	 * <p>应记录以下信息.
	 * <ul>
	 * <li>具体{@code ContextCache}实现的名称</li>
	 * <li>{@linkplain #size}</li>
	 * <li>{@linkplain #getParentContextCount() 父上下文计数}</li>
	 * <li>{@linkplain #getHitCount() 命中计数}</li>
	 * <li>{@linkplain #getMissCount() 未命中计数}</li>
	 * <li>用于监视此缓存状态的任何其他信息</li>
	 * </ul>
	 */
	void logStatistics();

}
