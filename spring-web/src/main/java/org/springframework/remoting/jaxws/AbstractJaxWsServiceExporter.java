package org.springframework.remoting.jaxws;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceProvider;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JAX-WS服务的抽象导出器, 自动检测带注解的服务bean (通过JAX-WS {@link javax.jws.WebService}注解).
 * 兼容JAX-WS 2.1和2.2, 包含在JDK 6 update 4+和Java 7/8中.
 *
 * <p>子类需要为实际端点暴露实现{@link #publishEndpoint}模板方法.
 */
public abstract class AbstractJaxWsServiceExporter implements BeanFactoryAware, InitializingBean, DisposableBean {

	private Map<String, Object> endpointProperties;

	private Executor executor;

	private String bindingType;

	private WebServiceFeature[] endpointFeatures;

	private Object[] webServiceFeatures;

	private ListableBeanFactory beanFactory;

	private final Set<Endpoint> publishedEndpoints = new LinkedHashSet<Endpoint>();


	/**
	 * 设置端点的属性包, 包括"javax.xml.ws.wsdl.service"或"javax.xml.ws.wsdl.port"等属性.
	 */
	public void setEndpointProperties(Map<String, Object> endpointProperties) {
		this.endpointProperties = endpointProperties;
	}

	/**
	 * 设置JDK并发执行器, 以用于将传入请求分派给导出的服务实例.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 指定要使用的绑定类型, 覆盖JAX-WS {@link javax.xml.ws.BindingType}注解的值.
	 */
	public void setBindingType(String bindingType) {
		this.bindingType = bindingType;
	}

	/**
	 * 指定要应用于JAX-WS端点创建的WebServiceFeature对象 (e.g. 作为内部bean定义).
	 */
	public void setEndpointFeatures(WebServiceFeature... endpointFeatures) {
		this.endpointFeatures = endpointFeatures;
	}

	/**
	 * 允许提供JAX-WS 2.2 WebServiceFeature规范:
	 * 以实际的{@link javax.xml.ws.WebServiceFeature}对象, WebServiceFeature Class引用, 或WebServiceFeature类名的形式.
	 * <p>从Spring 4.0开始, 这实际上只是指定{@link #setEndpointFeatures "endpointFeatures"}的另一种方法.
	 * 不要同时指定这两个属性; 更喜欢"endpointFeatures".
	 * 
	 * @deprecated as of Spring 4.0, in favor of {@link #setEndpointFeatures}
	 */
	@Deprecated
	public void setWebServiceFeatures(Object[] webServiceFeatures) {
		this.webServiceFeatures = webServiceFeatures;
	}

	/**
	 * 获取所有Web服务bean, 并将它们作为JAX-WS端点发布.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalStateException(getClass().getSimpleName() + " requires a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}


	/**
	 * 完全配置后立即发布所有端点.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		publishEndpoints();
	}

	/**
	 * 在BeanFactory中发布所有带{@link javax.jws.WebService}注解的bean.
	 */
	public void publishEndpoints() {
		Set<String> beanNames = new LinkedHashSet<String>(this.beanFactory.getBeanDefinitionCount());
		beanNames.addAll(Arrays.asList(this.beanFactory.getBeanDefinitionNames()));
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			beanNames.addAll(Arrays.asList(((ConfigurableBeanFactory) this.beanFactory).getSingletonNames()));
		}
		for (String beanName : beanNames) {
			try {
				Class<?> type = this.beanFactory.getType(beanName);
				if (type != null && !type.isInterface()) {
					WebService wsAnnotation = type.getAnnotation(WebService.class);
					WebServiceProvider wsProviderAnnotation = type.getAnnotation(WebServiceProvider.class);
					if (wsAnnotation != null || wsProviderAnnotation != null) {
						Endpoint endpoint = createEndpoint(this.beanFactory.getBean(beanName));
						if (this.endpointProperties != null) {
							endpoint.setProperties(this.endpointProperties);
						}
						if (this.executor != null) {
							endpoint.setExecutor(this.executor);
						}
						if (wsAnnotation != null) {
							publishEndpoint(endpoint, wsAnnotation);
						}
						else {
							publishEndpoint(endpoint, wsProviderAnnotation);
						}
						this.publishedEndpoints.add(endpoint);
					}
				}
			}
			catch (CannotLoadBeanClassException ex) {
				// ignore beans where the class is not resolvable
			}
		}
	}

	/**
	 * 创建实际的Endpoint实例.
	 * 
	 * @param bean 要包装的服务对象
	 * 
	 * @return Endpoint实例
	 */
	@UsesJava7  // optional use of Endpoint#create with WebServiceFeature[]
	protected Endpoint createEndpoint(Object bean) {
		if (this.endpointFeatures != null || this.webServiceFeatures != null) {
			WebServiceFeature[] endpointFeaturesToUse = this.endpointFeatures;
			if (endpointFeaturesToUse == null) {
				endpointFeaturesToUse = new WebServiceFeature[this.webServiceFeatures.length];
				for (int i = 0; i < this.webServiceFeatures.length; i++) {
					endpointFeaturesToUse[i] = convertWebServiceFeature(this.webServiceFeatures[i]);
				}
			}
			return Endpoint.create(this.bindingType, bean, endpointFeaturesToUse);
		}
		else {
			return Endpoint.create(this.bindingType, bean);
		}
	}

	private WebServiceFeature convertWebServiceFeature(Object feature) {
		Assert.notNull(feature, "WebServiceFeature specification object must not be null");
		if (feature instanceof WebServiceFeature) {
			return (WebServiceFeature) feature;
		}
		else if (feature instanceof Class) {
			return (WebServiceFeature) BeanUtils.instantiate((Class<?>) feature);
		}
		else if (feature instanceof String) {
			try {
				Class<?> featureClass = getBeanClassLoader().loadClass((String) feature);
				return (WebServiceFeature) BeanUtils.instantiate(featureClass);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalArgumentException("Could not load WebServiceFeature class [" + feature + "]");
			}
		}
		else {
			throw new IllegalArgumentException("Unknown WebServiceFeature specification type: " + feature.getClass());
		}
	}

	private ClassLoader getBeanClassLoader() {
		return (beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) beanFactory).getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
	}


	/**
	 * 实际发布给定的端点. 由子类实现.
	 * 
	 * @param endpoint JAX-WS Endpoint对象
	 * @param annotation 服务bean的WebService注解
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebService annotation);

	/**
	 * 实际发布给定的提供者端点. 由子类实现.
	 * 
	 * @param endpoint JAX-WS Provider Endpoint对象
	 * @param annotation 服务bean的WebServiceProvider注解
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation);


	/**
	 * 停止所有已发布的端点, 使Web服务脱机.
	 */
	@Override
	public void destroy() {
		for (Endpoint endpoint : this.publishedEndpoints) {
			endpoint.stop();
		}
	}

}
