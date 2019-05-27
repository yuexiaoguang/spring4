package org.springframework.aop.framework;

import java.io.Closeable;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 具有代理处理器通用功能的基类, 特别是ClassLoader管理和{@link #evaluateProxyInterfaces}算法.
 */
@SuppressWarnings("serial")
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

	/**
	 * 这应该在所有其他处理器之后运行, 这样它就可以只为现有代理添加切面, 而不是双代理.
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;


	/**
	 * 设置适用于此处理器{@link Ordered}实现的顺序, 在应用多个处理器时使用.
	 * <p>默认是 {@code Ordered.LOWEST_PRECEDENCE}, 意味着没有顺序.
	 * 
	 * @param order 顺序值
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 设置ClassLoader以生成代理类.
	 * <p>默认是 bean ClassLoader, i.e. 包含{@link org.springframework.beans.factory.BeanFactory}的ClassLoader用于加载所有bean类.
	 * 对于特定代理，可以在此处覆盖此内容.
	 */
	public void setProxyClassLoader(ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	/**
	 * 返回此处理器的已配置代理ClassLoader.
	 */
	protected ClassLoader getProxyClassLoader() {
		return this.proxyClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}


	/**
	 * 检查给定bean类的接口并将它们应用于{@link ProxyFactory}, 如果合适的话.
	 * <p>调用 {@link #isConfigurationCallbackInterface} 和 {@link #isInternalLanguageInterface},
	 * 过滤合理的代理接口, 否则回到目标级代理.
	 * 
	 * @param beanClass bean的类
	 * @param proxyFactory bean的ProxyFactory
	 */
	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
		boolean hasReasonableProxyInterface = false;
		for (Class<?> ifc : targetInterfaces) {
			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
					ifc.getMethods().length > 0) {
				hasReasonableProxyInterface = true;
				break;
			}
		}
		if (hasReasonableProxyInterface) {
			// 必须允许引入; 不能只设置接口到目标接口.
			for (Class<?> ifc : targetInterfaces) {
				proxyFactory.addInterface(ifc);
			}
		}
		else {
			proxyFactory.setProxyTargetClass(true);
		}
	}

	/**
	 * 确定给定接口是否只是容器回调，因此不被视为合理的代理接口.
	 * <p>如果找不到给定bean的合理代理接口, 它将获得其完整目标类的代理, 假设这就是用户的意图.
	 * 
	 * @param ifc 要检查的接口
	 * 
	 * @return 给定的接口是否只是一个容器回调
	 */
	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
		return (InitializingBean.class == ifc || DisposableBean.class == ifc ||
				Closeable.class == ifc || "java.lang.AutoCloseable".equals(ifc.getName()) ||
				ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
	}

	/**
	 * 确定给定接口是否是众所周知的内部语言接口，因此不被视为合理的代理接口.
	 * <p>如果找不到给定bean的合理代理接口, 它将获得其完整目标类的代理, 假设这就是用户的意图.
	 * 
	 * @param ifc 要检查的接口
	 * 
	 * @return 给定的接口是否是内部语言接口
	 */
	protected boolean isInternalLanguageInterface(Class<?> ifc) {
		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
				ifc.getName().endsWith(".cglib.proxy.Factory") ||
				ifc.getName().endsWith(".bytebuddy.MockAccess"));
	}
}
