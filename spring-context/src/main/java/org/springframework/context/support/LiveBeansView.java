package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 实时bean视图的适配器, 从本地 {@code ApplicationContext} (带有本地 {@code LiveBeansView} bean定义)
 * 或所有已注册的 ApplicationContexts (由 {@value #MBEAN_DOMAIN_PROPERTY_NAME} 环境属性驱动),
 * 构建当前bean及其依赖项的快照.
 *
 * <p>Note: 此功能仍处于测试阶段, 主要用于Spring Tool Suite 3.1及更高版本.
 */
public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

	public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

	public static final String MBEAN_APPLICATION_KEY = "application";

	private static final Set<ConfigurableApplicationContext> applicationContexts =
			new LinkedHashSet<ConfigurableApplicationContext>();

	private static String applicationName;


	static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
		String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
		if (mbeanDomain != null) {
			synchronized (applicationContexts) {
				if (applicationContexts.isEmpty()) {
					try {
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						applicationName = applicationContext.getApplicationName();
						server.registerMBean(new LiveBeansView(),
								new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
					catch (Throwable ex) {
						throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
					}
				}
				applicationContexts.add(applicationContext);
			}
		}
	}

	static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (applicationContexts) {
			if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
				try {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
					server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
				}
				catch (Throwable ex) {
					throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
				}
				finally {
					applicationName = null;
				}
			}
		}
	}


	private ConfigurableApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}


	/**
	 * 生成当前bean及其依赖项的JSON快照, 通过{@link #findApplicationContexts()}查找所有活动的ApplicationContext,
	 * 然后委托给 {@link #generateJson(java.util.Set)}.
	 */
	@Override
	public String getSnapshotAsJson() {
		Set<ConfigurableApplicationContext> contexts;
		if (this.applicationContext != null) {
			contexts = Collections.singleton(this.applicationContext);
		}
		else {
			contexts = findApplicationContexts();
		}
		return generateJson(contexts);
	}

	/**
	 * 查找当前应用程序的所有适用ApplicationContext.
	 * <p>如果没有为此LiveBeansView设置特定的ApplicationContext, 则调用此方法.
	 * 
	 * @return ApplicationContext集合
	 */
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		synchronized (applicationContexts) {
			return new LinkedHashSet<ConfigurableApplicationContext>(applicationContexts);
		}
	}

	/**
	 * 实际在给定的ApplicationContexts中生成bean的JSON快照.
	 * <p>此实现不使用任何JSON解析库, 以避免第三方库依赖性.
	 * 它生成一组上下文描述对象, 每个对象包含一个context和parent属性, 以及一个带有嵌套bean描述对象的beans属性.
	 * 每个bean对象都包含一个 bean, scope, type 和 resource 属性, 以及一个 dependencies 属性, 该属性具有当前bean依赖的bean名称的嵌套数组.
	 * 
	 * @param contexts 一组ApplicationContext
	 * 
	 * @return JSON文档
	 */
	protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
		StringBuilder result = new StringBuilder("[\n");
		for (Iterator<ConfigurableApplicationContext> it = contexts.iterator(); it.hasNext();) {
			ConfigurableApplicationContext context = it.next();
			result.append("{\n\"context\": \"").append(context.getId()).append("\",\n");
			if (context.getParent() != null) {
				result.append("\"parent\": \"").append(context.getParent().getId()).append("\",\n");
			}
			else {
				result.append("\"parent\": null,\n");
			}
			result.append("\"beans\": [\n");
			ConfigurableListableBeanFactory bf = context.getBeanFactory();
			String[] beanNames = bf.getBeanDefinitionNames();
			boolean elementAppended = false;
			for (String beanName : beanNames) {
				BeanDefinition bd = bf.getBeanDefinition(beanName);
				if (isBeanEligible(beanName, bd, bf)) {
					if (elementAppended) {
						result.append(",\n");
					}
					result.append("{\n\"bean\": \"").append(beanName).append("\",\n");
					result.append("\"aliases\": ");
					appendArray(result, bf.getAliases(beanName));
					result.append(",\n");
					String scope = bd.getScope();
					if (!StringUtils.hasText(scope)) {
						scope = BeanDefinition.SCOPE_SINGLETON;
					}
					result.append("\"scope\": \"").append(scope).append("\",\n");
					Class<?> beanType = bf.getType(beanName);
					if (beanType != null) {
						result.append("\"type\": \"").append(beanType.getName()).append("\",\n");
					}
					else {
						result.append("\"type\": null,\n");
					}
					result.append("\"resource\": \"").append(getEscapedResourceDescription(bd)).append("\",\n");
					result.append("\"dependencies\": ");
					appendArray(result, bf.getDependenciesForBean(beanName));
					result.append("\n}");
					elementAppended = true;
				}
			}
			result.append("]\n");
			result.append("}");
			if (it.hasNext()) {
				result.append(",\n");
			}
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * 确定指定的bean是否有资格包含在LiveBeansView JSON快照中.
	 * 
	 * @param beanName bean的名称
	 * @param bd 相应的bean定义
	 * @param bf 包含bean工厂
	 * 
	 * @return {@code true} 如果要包括bean; 否则{@code false}
	 */
	protected boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
		return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE &&
				(!bd.isLazyInit() || bf.containsSingleton(beanName)));
	}

	/**
	 * 确定给定bean定义的资源描述, 并对其应用基本JSON转义 (反斜杠, 双引号).
	 * 
	 * @param bd 用于构建资源描述的bean定义
	 * 
	 * @return JSON转义后的资源描述
	 */
	protected String getEscapedResourceDescription(BeanDefinition bd) {
		String resourceDescription = bd.getResourceDescription();
		if (resourceDescription == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(resourceDescription.length() + 16);
		for (int i = 0; i < resourceDescription.length(); i++) {
			char character = resourceDescription.charAt(i);
			if (character == '\\') {
				result.append('/');
			}
			else if (character == '"') {
				result.append("\\").append('"');
			}
			else {
				result.append(character);
			}
		}
		return result.toString();
	}

	private void appendArray(StringBuilder result, String[] arr) {
		result.append('[');
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(StringUtils.arrayToDelimitedString(arr, "\", \""));
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(']');
	}

}
