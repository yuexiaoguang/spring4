package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 用于为应用程序提供配置的中央接口.
 * 这在应用程序运行时是只读的, 但如果实现支持, 则可以重新加载.
 *
 * <p>ApplicationContext提供:
 * <ul>
 * <li>Bean工厂方法, 用于访问应用程序组件.
 * 继承自{@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>以通用方式加载文件资源的能力.
 * 继承自 {@link org.springframework.core.io.ResourceLoader}接口.
 * <li>将事件发布到已注册的监听器的功能.
 * 继承自 {@link ApplicationEventPublisher}接口.
 * <li>解析消息, 支持国际化的能力.
 * 继承自 {@link MessageSource}接口.
 * <li>从父级上下文继承. 后代上下文中的定义始终优先.
 * 这意味着, 例如, 整个Web应用程序可以使用单个父级上下文, 而每个servlet都有自己的子上下文, 该上下文独立于任何其他servlet的上下文.
 * </ul>
 *
 * <p>除了标准的 {@link org.springframework.beans.factory.BeanFactory}生命周期功能,
 * ApplicationContext 实现检测并调用 {@link ApplicationContextAware} bean以及 {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware}和 {@link MessageSourceAware} beans.
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回此应用程序上下文的唯一ID.
	 * 
	 * @return 上下文的唯一ID, 或{@code null}
	 */
	String getId();

	/**
	 * 返回此上下文所属的已部署的应用程序的名称.
	 * 
	 * @return 已部署的应用程序的名称, 或默认情况下为空String
	 */
	String getApplicationName();

	/**
	 * 返回此上下文的友好名称.
	 * 
	 * @return 此上下文的显示名称 (never {@code null})
	 */
	String getDisplayName();

	/**
	 * 返回首次加载此上下文时的时间戳.
	 * 
	 * @return 首次加载此上下文时的时间戳(ms)
	 */
	long getStartupDate();

	/**
	 * 返回父级上下文, 如果没有父级上下文, 则返回{@code null}, 这是上下文层次结构的根.
	 * 
	 * @return 父级上下文, 或{@code null}
	 */
	ApplicationContext getParent();

	/**
	 * 为此上下文公开AutowireCapableBeanFactory功能.
	 * <p>除了初始化位于应用程序上下文之外的bean实例之外, 应用程序代码通常不会使用它,
	 * 将Spring bean生命周期(全部或部分)应用于它们.
	 * <p>或者, {@link ConfigurableApplicationContext}接口公开的内部BeanFactory也可以访问{@link AutowireCapableBeanFactory}接口.
	 * 本方法主要用作ApplicationContext接口上方便的特定工具.
	 * <p><b>NOTE: 从4.2开始, 在关闭应用程序上下文后, 此方法将始终抛出IllegalStateException.</b>
	 * 在当前的Spring Framework版本中, 只有可刷新的应用程序上下文才有这种方式;
	 * 从4.2开始, 所有应用程序上下文实现都需要遵守.
	 * 
	 * @return 此上下文的AutowireCapableBeanFactory
	 * @throws IllegalStateException 如果上下文不支持{@link AutowireCapableBeanFactory}接口,
	 * 或者还没有一个支持autowire的bean工厂 (e.g. 如果从来没被{@code refresh()}), 或者已经关闭了上下文
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
