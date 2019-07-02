package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * 加载{@link GenericWebApplicationContext}的{@link AbstractContextLoader}的抽象通用扩展.
 *
 * <p>如果通过{@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI
 * 调用具体子类的实例, 则将从提供给{@link #loadContext(MergedContextConfiguration)}的{@link MergedContextConfiguration}加载上下文.
 * 在这种情况下, {@code SmartContextLoader}将决定是否从<em>locations</em>或<em>带注解的类</em>加载上下文.
 * 请注意, {@code AbstractGenericWebContextLoader}不支持传统
 * {@link org.springframework.test.context.ContextLoader ContextLoader} SPI中的
 * {@code loadContext(String... locations)}方法.
 *
 * <p>具体的子类必须提供{@link #loadBeanDefinitions}的适当实现.
 */
public abstract class AbstractGenericWebContextLoader extends AbstractContextLoader {

	protected static final Log logger = LogFactory.getLog(AbstractGenericWebContextLoader.class);


	// SmartContextLoader

	/**
	 * 从提供的{@link MergedContextConfiguration}加载Spring {@link WebApplicationContext}.
	 * <p>实现详情:
	 * <ul>
	 * <li>调用{@link #validateMergedContextConfiguration(WebMergedContextConfiguration)}
	 * 以允许子类在继续之前验证提供的配置.</li>
	 * <li>创建{@link GenericWebApplicationContext}实例.</li>
	 * <li>如果提供的{@code MergedContextConfiguration}引用了
	 * {@linkplain MergedContextConfiguration#getParent() 父级配置},
	 * 则将检索相应的{@link MergedContextConfiguration#getParentApplicationContext() ApplicationContext},
	 * 并为此方法创建的上下文{@linkplain GenericWebApplicationContext#setParent(ApplicationContext) 设置父级}.</li>
	 * <li>委托给{@link #configureWebResources} 创建{@link MockServletContext}并将其设置在{@code WebApplicationContext}中.</li>
	 * <li>调用{@link #prepareContext}以允许在加载bean定义之前自定义上下文.</li>
	 * <li>调用{@link #customizeBeanFactory}以允许自定义上下文的{@code DefaultListableBeanFactory}.</li>
	 * <li>委托给{@link #loadBeanDefinitions}从提供的{@code MergedContextConfiguration}中的位置或类填充上下文.</li>
	 * <li>委托给{@link AnnotationConfigUtils}以
	 * {@linkplain AnnotationConfigUtils#registerAnnotationConfigProcessors 注册}注解配置处理器.</li>
	 * <li>调用{@link #customizeContext}以允许在刷新上下文之前自定义上下文.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh 刷新}上下文并为其注册JVM关闭挂钩.</li>
	 * </ul>
	 * 
	 * @return 新的Web应用程序上下文
	 */
	@Override
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		if (!(mergedConfig instanceof WebMergedContextConfiguration)) {
			throw new IllegalArgumentException(String.format(
					"Cannot load WebApplicationContext from non-web merged context configuration %s. " +
					"Consider annotating your test class with @WebAppConfiguration.", mergedConfig));
		}
		WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) mergedConfig;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading WebApplicationContext for merged context configuration %s.",
				webMergedConfig));
		}

		validateMergedContextConfiguration(webMergedConfig);

		GenericWebApplicationContext context = new GenericWebApplicationContext();

		ApplicationContext parent = mergedConfig.getParentApplicationContext();
		if (parent != null) {
			context.setParent(parent);
		}
		configureWebResources(context, webMergedConfig);
		prepareContext(context, webMergedConfig);
		customizeBeanFactory(context.getDefaultListableBeanFactory(), webMergedConfig);
		loadBeanDefinitions(context, webMergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context, webMergedConfig);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * 根据此上下文加载器支持的内容验证提供的{@link WebMergedContextConfiguration}.
	 * <p>默认实现是<em>no-op</em>, 但可以根据需要由子类覆盖.
	 * 
	 * @param mergedConfig 要验证的合并配置
	 * 
	 * @throws IllegalStateException 如果提供的配置对此上下文加载器无效
	 */
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration mergedConfig) {
		/* no-op */
	}

	/**
	 * 为所提供的Web应用程序上下文 (WAC)配置Web资源.
	 * <h4>实现详情</h4>
	 * <p>如果提供的WAC没有父级或其父级不是WAC, 则提供的WAC将配置为根WAC (请参阅下面的"<em>Root WAC Configuration</em>").
	 * <p>否则, 将遍历所提供的WAC的上下文层次结构以找到最顶层的WAC (i.e., 根);
	 * 并且根WAC的{@link ServletContext}将被设置为所提供的WAC的{@code ServletContext}.
	 * <h4>根WAC配置</h4>
	 * <ul>
	 * <li>从提供的{@code WebMergedContextConfiguration}中检索资源基本路径.</li>
	 * <li>为{@link MockServletContext}实例化{@link ResourceLoader}:
	 * 如果资源基路径以"{@code classpath:}"为前缀, 则将使用{@link DefaultResourceLoader};
	 * 否则, 将使用{@link FileSystemResourceLoader}.</li>
	 * <li>将使用资源基础路径和资源加载器创建{@code MockServletContext}.</li>
	 * <li>然后将提供的{@link GenericWebApplicationContext}存储在以
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}为键的{@code MockServletContext}中.</li>
	 * <li>最后, 在{@code WebApplicationContext}中设置{@code MockServletContext}.</li>
	 * 
	 * @param context 要为其配置Web资源的Web应用程序上下文
	 * @param webMergedConfig 用于加载Web应用程序上下文的合并上下文配置
	 */
	protected void configureWebResources(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {

		ApplicationContext parent = context.getParent();

		// 如果WebApplicationContext没有父项或父项不是WebApplicationContext, 请将当前上下文设置为根WebApplicationContext:
		if (parent == null || (!(parent instanceof WebApplicationContext))) {
			String resourceBasePath = webMergedConfig.getResourceBasePath();
			ResourceLoader resourceLoader = (resourceBasePath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ?
					new DefaultResourceLoader() : new FileSystemResourceLoader());
			ServletContext servletContext = new MockServletContext(resourceBasePath, resourceLoader);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
			context.setServletContext(servletContext);
		}
		else {
			ServletContext servletContext = null;
			// 查找根 WebApplicationContext
			while (parent != null) {
				if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
					servletContext = ((WebApplicationContext) parent).getServletContext();
					break;
				}
				parent = parent.getParent();
			}
			Assert.state(servletContext != null, "Failed to find root WebApplicationContext in the context hierarchy");
			context.setServletContext(servletContext);
		}
	}

	/**
	 * 自定义此上下文加载器创建的{@code WebApplicationContext}的内部bean工厂.
	 * <p>默认实现为空, 但可以在子类中重写以自定义{@code DefaultListableBeanFactory}的标准设置.
	 * 
	 * @param beanFactory 由此上下文加载器创建的bean工厂
	 * @param webMergedConfig 用于加载Web应用程序上下文的合并上下文配置
	 */
	protected void customizeBeanFactory(
			DefaultListableBeanFactory beanFactory, WebMergedContextConfiguration webMergedConfig) {
	}

	/**
	 * 从提供的{@code WebMergedContextConfiguration}中的位置或类将bean定义
	 * 加载到提供的{@link GenericWebApplicationContext context}中.
	 * <p>具体的子类必须提供适当的实现.
	 * 
	 * @param context 应该加载bean定义的上下文
	 * @param webMergedConfig 用于加载Web应用程序上下文的合并上下文配置
	 */
	protected abstract void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig);

	/**
	 * 在将bean定义加载到上下文<i>之后</i>, 但在刷新上下文<i>之前</i>,
	 * 自定义由此上下文加载器创建的{@link GenericWebApplicationContext}.
	 * <p>此实现委托给
	 * {@link AbstractContextLoader#customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)}.
	 * 
	 * @param context 新创建的Web应用程序上下文
	 * @param webMergedConfig 用于加载Web应用程序上下文的合并上下文配置
	 */
	protected void customizeContext(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		super.customizeContext(context, webMergedConfig);
	}


	// ContextLoader

	/**
	 * {@code AbstractGenericWebContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"AbstractGenericWebContextLoader does not support the loadContext(String... locations) method");
	}

}
