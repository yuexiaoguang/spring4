package org.springframework.beans.factory.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 用于创建单例或原型对象的{@link FactoryBean}实现的简单模板超类, 取决于一个标记.
 *
 * <p>如果"singleton"标记是 {@code true} (默认), 这个类将在初始化时只创建一次这个对象,
 * 然后在对{@link #getObject()}方法的所有调用都返回这个单例实例.
 *
 * <p>否则, 每次调用 {@link #getObject()}方法时, 此类都将创建一个新实例.
 * 子类负责实现抽象的 {@link #createInstance()} 模板方法, 以实际创建要公开的对象.
 */
public abstract class AbstractFactoryBean<T>
		implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean singleton = true;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private BeanFactory beanFactory;

	private boolean initialized = false;

	private T singletonInstance;

	private T earlySingletonInstance;


	/**
	 * 设置是否应创建单例, 否则每次请求都是新对象.
	 * 默认 {@code true} (单例).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回此bean运行的BeanFactory.
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 从Bean运行的BeanFactory中获取bean类型转换器.
	 * 通常是每个调用返回一个新实例, 因为TypeConverters通常不是线程安全的.
	 * <p>不在BeanFactory中运行时回退到SimpleTypeConverter.
	 */
	protected TypeConverter getBeanTypeConverter() {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
		}
		else {
			return new SimpleTypeConverter();
		}
	}

	/**
	 * 实时地创建单例实例.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (isSingleton()) {
			this.initialized = true;
			this.singletonInstance = createInstance();
			this.earlySingletonInstance = null;
		}
	}


	/**
	 * 公开单例实例或创建一个新的原型实例.
	 */
	@Override
	public final T getObject() throws Exception {
		if (isSingleton()) {
			return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
		}
		else {
			return createInstance();
		}
	}

	/**
	 * 确定 '实时单例'实例, 在循环引用的情况下公开. 不在非循环场景中调用.
	 */
	@SuppressWarnings("unchecked")
	private T getEarlySingletonInstance() throws Exception {
		Class<?>[] ifcs = getEarlySingletonInterfaces();
		if (ifcs == null) {
			throw new FactoryBeanNotInitializedException(
					getClass().getName() + " does not support circular references");
		}
		if (this.earlySingletonInstance == null) {
			this.earlySingletonInstance = (T) Proxy.newProxyInstance(
					this.beanClassLoader, ifcs, new EarlySingletonInvocationHandler());
		}
		return this.earlySingletonInstance;
	}

	/**
	 * 公开单例实例 (通过'实时单例'代理访问).
	 * 
	 * @return 此FactoryBean包含的单例实例
	 * @throws IllegalStateException 如果单例实例未初始化
	 */
	private T getSingletonInstance() throws IllegalStateException {
		Assert.state(this.initialized, "Singleton instance not initialized yet");
		return this.singletonInstance;
	}

	/**
	 * 销毁单例实例.
	 */
	@Override
	public void destroy() throws Exception {
		if (isSingleton()) {
			destroyInstance(this.singletonInstance);
		}
	}


	/**
	 * 此抽象方法声明镜像FactoryBean接口中的方法, 提供一致的抽象模板方法.
	 */
	@Override
	public abstract Class<?> getObjectType();

	/**
	 * 子类必须重写的模板方法, 以构造此工厂返回的对象.
	 * <p>在单例的情况下, 在初始化此FactoryBean时调用; 否则每次调用 {@link #getObject()}都会调用.
	 * 
	 * @return 该工厂返回的对象
	 * @throws Exception 如果在对象创建期间发生异常
	 */
	protected abstract T createInstance() throws Exception;

	/**
	 * 返回由此FactoryBean公开的单个对象应该实现的接口数组, 用于'实时单例代理', 在循环引用的情况下将被公开.
	 * <p>默认实现返回此FactoryBean的对象类型, 只要它是一个接口, 或{@code null}.
	 * 后者表示此FactoryBean不支持实时单例访问. 这将导致抛出FactoryBeanNotInitializedException.
	 * 
	 * @return 用于'实时单例'的接口, 或{@code null}抛出 FactoryBeanNotInitializedException
	 */
	protected Class<?>[] getEarlySingletonInterfaces() {
		Class<?> type = getObjectType();
		return (type != null && type.isInterface() ? new Class<?>[] {type} : null);
	}

	/**
	 * 用于销毁单例实例的回调. 子类可以重写此方法以销毁先前创建的实例.
	 * <p>默认实现为空.
	 * 
	 * @param instance {@link #createInstance()}返回的单例实例
	 * 
	 * @throws Exception 关闭错误
	 */
	protected void destroyInstance(T instance) throws Exception {
	}


	/**
	 * Reflective InvocationHandler用于延迟访问实际的单例对象.
	 */
	private class EarlySingletonInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// Use hashCode of reference proxy.
				return System.identityHashCode(proxy);
			}
			else if (!initialized && ReflectionUtils.isToStringMethod(method)) {
				return "Early singleton proxy for interfaces " +
						ObjectUtils.nullSafeToString(getEarlySingletonInterfaces());
			}
			try {
				return method.invoke(getSingletonInstance(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}
}
