package org.springframework.jca.context;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * JCA 1.5 {@link javax.resource.spi.ResourceAdapter}实现,
 * 加载Spring {@link org.springframework.context.ApplicationContext},
 * 启动和停止Spring管理的bean作为ResourceAdapter生命周期的一部分.
 *
 * <p>非常适合不需要任何HTTP入口点, 只包含消息端点和定时任务等的应用程序上下文.
 * 在这样的上下文中的Bean可以使用应用程序服务器资源, 例如JTA事务管理器和JNDI绑定的JDBC DataSources和JMS ConnectionFactory实例,
 * 也可以注册平台的JMX服务器 - 所有这些都通过Spring的标准事务管理以及JNDI和JMX支持工具完成.
 *
 * <p>如果需要调度异步工作, 请考虑使用Spring的{@link org.springframework.jca.work.WorkManagerTaskExecutor}作为标准bean定义,
 * 通过依赖注入将其注入应用程序bean.
 * 此WorkManagerTaskExecutor将自动使用已提供给此ResourceAdapter的BootstrapContext中的JCA WorkManager.
 *
 * <p>也可以通过实现{@link BootstrapContextAware}接口的应用程序组件直接访问JCA {@link javax.resource.spi.BootstrapContext}.
 * 使用此ResourceAdapter进行部署时, 可确保将BootstrapContext传递给此类组件.
 *
 * <p>此ResourceAdapter将在J2EE ".rar"部署单元中的"META-INF/ra.xml"文件中定义, 如下所示:
 *
 * <pre class="code">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;connector xmlns="http://java.sun.com/xml/ns/j2ee"
 *		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *		 xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/connector_1_5.xsd"
 *		 version="1.5"&gt;
 *	 &lt;vendor-name&gt;Spring Framework&lt;/vendor-name&gt;
 *	 &lt;eis-type&gt;Spring Connector&lt;/eis-type&gt;
 *	 &lt;resourceadapter-version&gt;1.0&lt;/resourceadapter-version&gt;
 *	 &lt;resourceadapter&gt;
 *		 &lt;resourceadapter-class&gt;org.springframework.jca.context.SpringContextResourceAdapter&lt;/resourceadapter-class&gt;
 *		 &lt;config-property&gt;
 *			 &lt;config-property-name&gt;ContextConfigLocation&lt;/config-property-name&gt;
 *			 &lt;config-property-type&gt;java.lang.String&lt;/config-property-type&gt;
 *			 &lt;config-property-value&gt;META-INF/applicationContext.xml&lt;/config-property-value&gt;
 *		 &lt;/config-property&gt;
 *	 &lt;/resourceadapter&gt;
 * &lt;/connector&gt;</pre>
 *
 * 请注意, "META-INF/applicationContext.xml"是默认的上下文配置位置, 因此除非打算指定不同的/额外的配置文件, 否则不必指定它.
 * 因此, 在默认情况下, 可以删除上面的整个{@code config-property}部分.
 *
 * <p><b>对于简单的部署需求, 需要做的就是以下内容:</b>
 * 将所有应用程序类打包到一个RAR文件 (它只是一个具有不同文件扩展名的标准JAR文件)中,
 * 将所有必需的库jar添加到RAR存档的根目录中, 添加如上所示的"META-INF/ra.xml"部署描述符,
 * 以及相应的Spring XML bean定义文件 (通常是"META-INF/applicationContext.xml"),
 * 并将生成的RAR文件放入应用程序服务器的部署目录中!
 */
public class SpringContextResourceAdapter implements ResourceAdapter {

	/**
	 * 任何数量的这些字符都被视为单个String值中多个上下文配置路径之间的分隔符.
	 */
	public static final String CONFIG_LOCATION_DELIMITERS = ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;

	public static final String DEFAULT_CONTEXT_CONFIG_LOCATION = "META-INF/applicationContext.xml";


	protected final Log logger = LogFactory.getLog(getClass());

	private String contextConfigLocation = DEFAULT_CONTEXT_CONFIG_LOCATION;

	private ConfigurableApplicationContext applicationContext;


	/**
	 * 在资源适配器的部署单元中设置上下文配置文件的位置.
	 * 这可以是由多个资源位置组成的分隔字符串, 以逗号, 分号, 空格或换行符分隔.
	 * <p>这可以在{@code ra.xml}部署描述符中指定为"ContextConfigLocation"配置属性.
	 * <p>默认是"classpath:META-INF/applicationContext.xml".
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回指定的上下文配置文件.
	 */
	protected String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 返回新的{@link StandardEnvironment}.
	 * <p>子类可以覆盖此方法, 以便提供自定义的{@link ConfigurableEnvironment}实现.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * 此实现通过{@link #createApplicationContext}模板方法加载Spring ApplicationContext.
	 */
	@Override
	public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting SpringContextResourceAdapter with BootstrapContext: " + bootstrapContext);
		}
		this.applicationContext = createApplicationContext(bootstrapContext);
	}

	/**
	 * 为给定的JCA BootstrapContext构建Spring ApplicationContext.
	 * <p>默认实现构建{@link ResourceAdapterApplicationContext}
	 * 并委托给{@link #loadBeanDefinitions}以实际解析指定的配置文件.
	 * 
	 * @param bootstrapContext 这个ResourceAdapter的BootstrapContext
	 * 
	 * @return Spring ApplicationContext实例
	 */
	protected ConfigurableApplicationContext createApplicationContext(BootstrapContext bootstrapContext) {
		ResourceAdapterApplicationContext applicationContext =
				new ResourceAdapterApplicationContext(bootstrapContext);
		// 将ResourceAdapter的ClassLoader设置为bean类加载器.
		applicationContext.setClassLoader(getClass().getClassLoader());
		// 提取单个配置位置.
		String[] configLocations =
				StringUtils.tokenizeToStringArray(getContextConfigLocation(), CONFIG_LOCATION_DELIMITERS);
		if (configLocations != null) {
			loadBeanDefinitions(applicationContext, configLocations);
		}
		applicationContext.refresh();
		return applicationContext;
	}

	/**
	 * 根据指定的配置文件将bean定义加载到给定的注册表中.
	 * 
	 * @param registry 加载到的注册表
	 * @param configLocations 解析的配置位置
	 */
	protected void loadBeanDefinitions(BeanDefinitionRegistry registry, String[] configLocations) {
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(configLocations);
	}

	/**
	 * 此实现关闭Spring ApplicationContext.
	 */
	@Override
	public void stop() {
		logger.info("Stopping SpringContextResourceAdapter");
		this.applicationContext.close();
	}


	/**
	 * 此实现始终抛出NotSupportedException.
	 */
	@Override
	public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec)
			throws ResourceException {

		throw new NotSupportedException("SpringContextResourceAdapter does not support message endpoints");
	}

	/**
	 * 此实现为空.
	 */
	@Override
	public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	/**
	 * 此实现总是返回{@code null}.
	 */
	@Override
	public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
		return null;
	}


	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SpringContextResourceAdapter &&
				ObjectUtils.nullSafeEquals(getContextConfigLocation(),
						((SpringContextResourceAdapter) obj).getContextConfigLocation()));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getContextConfigLocation());
	}
}
