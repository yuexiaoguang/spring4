package org.springframework.web.servlet.view.tiles3;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.el.ELAttributeEvaluator;
import org.apache.tiles.el.ScopeELResolver;
import org.apache.tiles.el.TilesContextBeanELResolver;
import org.apache.tiles.el.TilesContextELResolver;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.ApplicationContextAware;
import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.startup.DefaultTilesInitializer;
import org.apache.tiles.startup.TilesInitializer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * 为Spring Framework配置Tiles 3.x的Helper类.
 * 有关Tiles的更多信息, 请参阅
 * <a href="http://tiles.apache.org">http://tiles.apache.org</a>
 * 它基本上是使用JSP和其他模板引擎的Web应用程序的模板机制.
 *
 * <p>TilesConfigurer只使用包含定义的一组文件配置TilesContainer, 以便{@link TilesView}实例访问.
 * 这是一个基于Spring的 (用于Spring配置) 用于Tiles提供的{@code ServletContextListener}的替代方法
 * (e.g. {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesListener}用于{@code web.xml}).
 *
 * <p>任何{@link org.springframework.web.servlet.ViewResolver}都可以管理TilesViews.
 * 对于简单的基于约定的视图解析, 考虑使用{@link TilesViewResolver}.
 *
 * <p>典型的TilesConfigurer bean定义如下所示:
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer">
 *   &lt;property name="definitions">
 *     &lt;list>
 *       &lt;value>/WEB-INF/defs/general.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/widgets.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/administrator.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/customer.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/templates.xml&lt;/value>
 *     &lt;/list>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * 列表中的值是包含定义的实际Tiles XML文件.
 * 如果未指定列表, 则默认为{@code "/WEB-INF/tiles.xml"}.
 *
 * <p>请注意, 在Tiles 3中, 包含Tiles定义的文件名称中的下划线用于表示区域设置信息, 例如:
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer">
 *   &lt;property name="definitions">
 *     &lt;list>
 *       &lt;value>/WEB-INF/defs/tiles.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/tiles_fr_FR.xml&lt;/value>
 *     &lt;/list>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 */
public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

	private static final boolean tilesElPresent =
			ClassUtils.isPresent("org.apache.tiles.el.ELAttributeEvaluator", TilesConfigurer.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	private TilesInitializer tilesInitializer;

	private String[] definitions;

	private boolean checkRefresh = false;

	private boolean validateDefinitions = true;

	private Class<? extends DefinitionsFactory> definitionsFactoryClass;

	private Class<? extends PreparerFactory> preparerFactoryClass;

	private boolean useMutableTilesContainer = false;

	private ServletContext servletContext;


	/**
	 * 使用自定义TilesInitializer配置Tiles, 通常指定为内部bean.
	 * <p>默认为{@link org.apache.tiles.startup.DefaultTilesInitializer}的变体,
	 * 尊重此配置器上的"definitions", "preparerFactoryClass"等属性.
	 * <p><b>NOTE: 指定自定义TilesInitializer会有效地禁用此配置器上的所有其他Bean属性.</b>
	 * 然后将整个初始化过程留给指定的TilesInitializer.
	 */
	public void setTilesInitializer(TilesInitializer tilesInitializer) {
		this.tilesInitializer = tilesInitializer;
	}

	/**
	 * 指定是否应用Tiles 3.0的"complete-autoload"配置.
	 * <p>有关完整自动加载模式的详细信息, 请参阅{@link org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory}.
	 * <p><b>NOTE: 指定complete-autoload模式可以有效地禁用此配置器上的所有其他bean属性.</b>
	 * 然后将整个初始化过程留给{@link org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer}.
	 */
	public void setCompleteAutoload(boolean completeAutoload) {
		if (completeAutoload) {
			try {
				this.tilesInitializer = new SpringCompleteAutoloadTilesInitializer();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Tiles-Extras 3.0 not available", ex);
			}
		}
		else {
			this.tilesInitializer = null;
		}
	}

	/**
	 * 设置Tiles定义, i.e. 包含定义的文件列表.
	 * 默认为"/WEB-INF/tiles.xml".
	 */
	public void setDefinitions(String... definitions) {
		this.definitions = definitions;
	}

	/**
	 * 设置是否在运行时检查Tiles定义文件以进行刷新.
	 * 默认为"false".
	 */
	public void setCheckRefresh(boolean checkRefresh) {
		this.checkRefresh = checkRefresh;
	}

	/**
	 * 设置是否验证Tiles XML定义. 默认为"true".
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.validateDefinitions = validateDefinitions;
	}

	/**
	 * 设置要使用的{@link org.apache.tiles.definition.DefinitionsFactory}实现.
	 * 默认为{@link org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory}, 在定义资源URL上运行.
	 * <p>指定自定义的DefinitionsFactory, e.g. UrlDefinitionsFactory子类, 用于自定义Tiles Definition对象的创建.
	 * 请注意, 除非配置不同的TilesContainerFactory, 否则这样的DefinitionsFactory必须能够处理{@link java.net.URL}源对象.
	 */
	public void setDefinitionsFactoryClass(Class<? extends DefinitionsFactory> definitionsFactoryClass) {
		this.definitionsFactoryClass = definitionsFactoryClass;
	}

	/**
	 * 设置要使用的{@link org.apache.tiles.preparer.factory.PreparerFactory}实现.
	 * 默认为{@link org.apache.tiles.preparer.factory.BasicPreparerFactory}, 为指定的preparer类创建共享实例.
	 * <p>指定{@link SimpleSpringPreparerFactory}以基于指定的preparer类自动装配{@link org.apache.tiles.preparer.ViewPreparer}实例,
	 * 应用Spring的容器回调以及应用配置的Spring BeanPostProcessor.
	 * 如果已激活Spring的上下文范围的annotation-config, 则将自动检测并应用ViewPreparer类中的注解.
	 * <p>指定{@link SpringBeanPreparerFactory}来操作指定的preparer <i>名称</i>, 而不是类,
	 * 从DispatcherServlet的应用程序上下文中获取相应的Spring bean.
	 * 在这种情况下, 完整的bean创建过程将控制在Spring应用程序上下文中, 允许使用scoped bean等.
	 * 请注意, 需要为每个preparer名称定义一个Spring bean定义 (在Tiles定义中使用).
	 */
	public void setPreparerFactoryClass(Class<? extends PreparerFactory> preparerFactoryClass) {
		this.preparerFactoryClass = preparerFactoryClass;
	}

	/**
	 * 设置是否为此应用程序使用MutableTilesContainer (通常是CachingTilesContainer实现).
	 * 默认为"false".
	 */
	public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
		this.useMutableTilesContainer = useMutableTilesContainer;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 为此Web应用程序创建并公开TilesContainer, 委托给TilesInitializer.
	 * 
	 * @throws TilesException 设置失败
	 */
	@Override
	public void afterPropertiesSet() throws TilesException {
		ApplicationContext preliminaryContext = new SpringWildcardServletTilesApplicationContext(this.servletContext);
		if (this.tilesInitializer == null) {
			this.tilesInitializer = new SpringTilesInitializer();
		}
		this.tilesInitializer.initialize(preliminaryContext);
	}

	/**
	 * 从此Web应用程序中删除TilesContainer.
	 * 
	 * @throws TilesException 清除失败
	 */
	@Override
	public void destroy() throws TilesException {
		this.tilesInitializer.destroy();
	}


	private class SpringTilesInitializer extends DefaultTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringTilesContainerFactory();
		}
	}


	private class SpringTilesContainerFactory extends BasicTilesContainerFactory {

		@Override
		protected TilesContainer createDecoratedContainer(TilesContainer originalContainer, ApplicationContext context) {
			return (useMutableTilesContainer ? new CachingTilesContainer(originalContainer) : originalContainer);
		}

		@Override
		protected List<ApplicationResource> getSources(ApplicationContext applicationContext) {
			if (definitions != null) {
				List<ApplicationResource> result = new LinkedList<ApplicationResource>();
				for (String definition : definitions) {
					Collection<ApplicationResource> resources = applicationContext.getResources(definition);
					if (resources != null) {
						result.addAll(resources);
					}
				}
				return result;
			}
			else {
				return super.getSources(applicationContext);
			}
		}

		@Override
		protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(ApplicationContext applicationContext,
				LocaleResolver resolver) {
			BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(applicationContext, resolver);
			if (checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
				((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(true);
			}
			return dao;
		}

		@Override
		protected DefinitionsReader createDefinitionsReader(ApplicationContext context) {
			DigesterDefinitionsReader reader = (DigesterDefinitionsReader) super.createDefinitionsReader(context);
			reader.setValidating(validateDefinitions);
			return reader;
		}

		@Override
		protected DefinitionsFactory createDefinitionsFactory(ApplicationContext applicationContext,
				LocaleResolver resolver) {

			if (definitionsFactoryClass != null) {
				DefinitionsFactory factory = BeanUtils.instantiate(definitionsFactoryClass);
				if (factory instanceof ApplicationContextAware) {
					((ApplicationContextAware) factory).setApplicationContext(applicationContext);
				}
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
				if (bw.isWritableProperty("localeResolver")) {
					bw.setPropertyValue("localeResolver", resolver);
				}
				if (bw.isWritableProperty("definitionDAO")) {
					bw.setPropertyValue("definitionDAO", createLocaleDefinitionDao(applicationContext, resolver));
				}
				return factory;
			}
			else {
				return super.createDefinitionsFactory(applicationContext, resolver);
			}
		}

		@Override
		protected PreparerFactory createPreparerFactory(ApplicationContext context) {
			if (preparerFactoryClass != null) {
				return BeanUtils.instantiate(preparerFactoryClass);
			}
			else {
				return super.createPreparerFactory(context);
			}
		}

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext context) {
			return new SpringLocaleResolver();
		}

		@Override
		protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(ApplicationContext context,
				LocaleResolver resolver) {
			AttributeEvaluator evaluator;
			if (tilesElPresent && JspFactory.getDefaultFactory() != null) {
				evaluator = new TilesElActivator().createEvaluator();
			}
			else {
				evaluator = new DirectAttributeEvaluator();
			}
			return new BasicAttributeEvaluatorFactory(evaluator);
		}
	}


	private static class SpringCompleteAutoloadTilesInitializer extends CompleteAutoloadTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringCompleteAutoloadTilesContainerFactory();
		}
	}


	private static class SpringCompleteAutoloadTilesContainerFactory extends CompleteAutoloadTilesContainerFactory {

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext applicationContext) {
			return new SpringLocaleResolver();
		}
	}


	private class TilesElActivator {

		public AttributeEvaluator createEvaluator() {
			ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
			evaluator.setExpressionFactory(
					JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory());
			evaluator.setResolver(new CompositeELResolverImpl());
			return evaluator;
		}
	}


	private static class CompositeELResolverImpl extends CompositeELResolver {

		public CompositeELResolverImpl() {
			add(new ScopeELResolver());
			add(new TilesContextELResolver(new TilesContextBeanELResolver()));
			add(new TilesContextBeanELResolver());
			add(new ArrayELResolver(false));
			add(new ListELResolver(false));
			add(new MapELResolver(false));
			add(new ResourceBundleELResolver());
			add(new BeanELResolver(false));
		}
	}

}
