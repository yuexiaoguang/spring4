package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.StringUtils;

/**
 * 加载{@link GenericApplicationContext}的{@link AbstractContextLoader}的抽象通用扩展.
 *
 * <ul>
 * <li>如果通过
 * {@link org.springframework.test.context.ContextLoader ContextLoader} SPI调用具体子类的实例,
 * 则将从提供给{@link #loadContext(String...)}的<em>locations</em>加载上下文.</li>
 * <li>如果通过
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI调用具体子类的实例,
 * 则将从提供给{@link #loadContext(MergedContextConfiguration)}的{@link MergedContextConfiguration}加载上下文.
 * 在这种情况下, a {@code SmartContextLoader}将决定是否从<em>locations</em>或<em>带注解的类</em>加载上下文.</li>
 * </ul>
 *
 * <p>具体的子类必须提供
 * {@link #createBeanDefinitionReader createBeanDefinitionReader()}的适当实现,
 * 也可能会覆盖{@link #loadBeanDefinitions loadBeanDefinitions()}.
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericContextLoader.class);


	/**
	 * 从提供的{@link MergedContextConfiguration}加载Spring ApplicationContext.
	 *
	 * <p>实现详情:
	 *
	 * <ul>
	 * <li>调用{@link #validateMergedContextConfiguration(MergedContextConfiguration)}
	 * 以允许子类在继续之前验证提供的配置.</li>
	 * <li>创建一个{@link GenericApplicationContext}实例.</li>
	 * <li>如果提供的{@code MergedContextConfiguration}引用了
	 * {@linkplain MergedContextConfiguration#getParent() 父级配置},
	 * 则将检索相应的{@link MergedContextConfiguration#getParentApplicationContext() ApplicationContext},
	 * 并为此方法创建的上下文{@linkplain GenericApplicationContext#setParent(ApplicationContext) 设置父级}.</li>
	 * <li>调用{@link #prepareContext(GenericApplicationContext)}以向后兼容
	 * {@link org.springframework.test.context.ContextLoader ContextLoader} SPI.</li>
	 * <li>调用{@link #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}
	 * 以允许在加载bean定义之前自定义上下文.</li>
	 * <li>调用{@link #customizeBeanFactory(DefaultListableBeanFactory)}以允许自定义上下文的{@code DefaultListableBeanFactory}.</li>
	 * <li>委托给{@link #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)}
	 * 从提供的{@code MergedContextConfiguration}中的位置或类填充上下文.</li>
	 * <li>委托给{@link AnnotationConfigUtils}, 以
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器.</li>
	 * <li>调用{@link #customizeContext(GenericApplicationContext)}以允许在刷新上下文之前自定义上下文.</li>
	 * <li>调用{@link #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)}
	 * 以允许在刷新上下文之前自定义上下文.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文并为它注册一个JVM关闭钩子.</li>
	 * </ul>
	 *
	 * @return 新的应用程序上下文
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for merged context configuration [%s].",
				mergedConfig));
		}

		validateMergedContextConfiguration(mergedConfig);

		GenericApplicationContext context = new GenericApplicationContext();

		ApplicationContext parent = mergedConfig.getParentApplicationContext();
		if (parent != null) {
			context.setParent(parent);
		}
		prepareContext(context);
		prepareContext(context, mergedConfig);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		loadBeanDefinitions(context, mergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);
		customizeContext(context, mergedConfig);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * 根据此上下文加载器支持的内容验证提供的{@link MergedContextConfiguration}.
	 * <p>默认实现是<em>no-op</em>, 但可以根据需要由子类覆盖.
	 * 
	 * @param mergedConfig 要验证的合并配置
	 * 
	 * @throws IllegalStateException 如果提供的配置对此上下文加载器无效
	 */
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		/* no-op */
	}

	/**
	 * 从提供的{@code locations}加载Spring ApplicationContext.
	 *
	 * <p>实现详情:
	 *
	 * <ul>
	 * <li>创建一个{@link GenericApplicationContext}实例.</li>
	 * <li>调用{@link #prepareContext(GenericApplicationContext)}以允许在加载bean定义之前自定义上下文.</li>
	 * <li>调用{@link #customizeBeanFactory(DefaultListableBeanFactory)}
	 * 以允许自定义上下文的{@code DefaultListableBeanFactory}.</li>
	 * <li>委托给{@link #createBeanDefinitionReader(GenericApplicationContext)}创建一个{@link BeanDefinitionReader},
	 * 然后用于填充指定位置的上下文.</li>
	 * <li>委托给{@link AnnotationConfigUtils}, 以
	 * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器.</li>
	 * <li>调用{@link #customizeContext(GenericApplicationContext)}以允许在刷新上下文之前自定义上下文.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文并为它注册一个JVM关闭钩子.</li>
	 * </ul>
	 *
	 * <p><b>Note</b>: 此方法不提供为加载的上下文设置活动Bean定义配置文件的方法.
	 * See {@link #loadContext(MergedContextConfiguration)}
	 * and {@link AbstractContextLoader#prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)} for an alternative.
	 *
	 * @return 新的应用程序上下文
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading ApplicationContext for locations [%s].",
				StringUtils.arrayToCommaDelimitedString(locations)));
		}
		GenericApplicationContext context = new GenericApplicationContext();
		prepareContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		createBeanDefinitionReader(context).loadBeanDefinitions(locations);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * 准备由此{@code ContextLoader}创建的{@link GenericApplicationContext}.
	 * 在读取bean定义之前调用.
	 *
	 * <p>默认实现为空. 可以在子类中重写以自定义{@code GenericApplicationContext}的标准设置.
	 *
	 * @param context 应该准备的上下文
	 */
	protected void prepareContext(GenericApplicationContext context) {
	}

	/**
	 * 自定义由此{@code ContextLoader}创建的ApplicationContext的内部bean工厂.
	 *
	 * <p>默认实现为空, 但可以在子类中重写以自定义{@code DefaultListableBeanFactory}的标准设置.
	 *
	 * @param beanFactory 由{@code ContextLoader}创建的bean工厂
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * 从提供的{@code MergedContextConfiguration}中的位置或类
	 * 将bean定义加载到提供的{@link GenericApplicationContext context}中.
	 *
	 * <p>默认实现委托给{@link #createBeanDefinitionReader(GenericApplicationContext)}返回的{@link BeanDefinitionReader}
	 * 以{@link BeanDefinitionReader#loadBeanDefinitions(String) 加载} bean定义.
	 *
	 * <p>子类必须提供
	 * {@link #createBeanDefinitionReader(GenericApplicationContext)}的适当实现.
	 * 或者, 子类可以提供{@code createBeanDefinitionReader()}的<em>no-op</em>实现,
	 * 并覆盖此方法以提供用于加载或注册bean定义的自定义策略.
	 *
	 * @param context 应该加载bean定义的上下文
	 * @param mergedConfig 合并的上下文配置
	 */
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		createBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * 用于创建新的{@link BeanDefinitionReader}的工厂方法,
	 * 将bean定义加载到提供的{@link GenericApplicationContext context}中.
	 *
	 * @param context 应该创建{@code BeanDefinitionReader}的上下文
	 * 
	 * @return 用于提供的上下文的{@code BeanDefinitionReader}
	 */
	protected abstract BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context);

	/**
	 * 在将bean定义加载到上下文之后, 但在上下文刷新之前,
	 * 自定义由此{@code ContextLoader}创建的{@link GenericApplicationContext}.
	 *
	 * <p>默认实现为空, 但可以在子类中重写以自定义应用程序上下文.
	 *
	 * @param context 新创建的应用程序上下文
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}
}
