package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean}实现,
 * 它接受一个必须有一个或多个签名是{@code MyType xxx()} 或 {@code MyType xxx(MyIdType id)}的方法的接口 
 * (通常 {@code MyService getService()}或{@code MyService getService(String id)})
 * 并创建一个实现该接口的动态代理, 委托给底层 {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>此类服务定位器允许通过使用适当的自定义定位器接口,
 * 将调用代码与{@link org.springframework.beans.factory.BeanFactory} API分离.
 * 它们通常用于原型bean, 即用于为每次调用返回新实例的工厂方法.
 * 客户端通过setter或构造函数注入接收对服务定位器的引用, 以便能够按需调用定位器的工厂方法.
 * <b>对于单例bean, 最好使用目标bean的直接setter或构造函数注入.</b>
 *
 * <p>在调用no-arg的工厂方法或String类型的id为{@code null}或空字符串的单个arg的工厂方法时,
 * 如果工厂中只有一个bean与工厂方法的返回类型匹配, 则返回该bean,
 * 否则抛出一个 {@link org.springframework.beans.factory.NoSuchBeanDefinitionException}.
 *
 * <p>在使用非null(和非空)参数调用single-arg的工厂方法时,
 * 代理返回{@link org.springframework.beans.factory.BeanFactory#getBean(String)}调用的结果,
 * 使用传入的id的字符串化版本作为bean名称.
 *
 * <p>工厂方法参数通常是String, 但也可以是int或自定义枚举类型, 例如, 通过{@code toString}字符串化.
 * 如果在bean工厂中定义了相应的bean, 则生成的String可以作为bean名称使用.
 * 或者, 可以定义服务ID和bean名称之间的{@linkplain #setServiceMappings(java.util.Properties) 自定义映射}.
 *
 * <p>举例来说, 请考虑以下服务定位器接口.
 * 请注意, 此接口不依赖于任何Spring API.
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService();
 *}</pre>
 *
 * <p>基于XML的{@link org.springframework.beans.factory.BeanFactory}中的示例配置可能如下所示:
 *
 * <pre class="code">&lt;beans>
 *
 *   &lt;!-- Prototype bean since we have state -->
 *   &lt;bean id="myService" class="a.b.c.MyService" singleton="false"/>
 *
 *   &lt;!-- will lookup the above 'myService' bean by *TYPE* -->
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean">
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/>
 *   &lt;/bean>
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean">
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/>
 *   &lt;/bean>
 *
 *&lt;/beans></pre>
 *
 * <p>随之而来的{@code MyClientBean}类实现可能看起来像这样:
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService();
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>通过按名称查找bean的示例, 请考虑以下服务定位器接口.
 * 再次注意, 此接口不依赖于任何Spring API.
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService (String serviceName);
 *}</pre>
 *
 * <p>基于XML的{@link org.springframework.beans.factory.BeanFactory}中的示例配置可能如下所示:
 *
 * <pre class="code">&lt;beans>
 *
 *   &lt;!-- Prototype beans since we have state (both extend MyService) -->
 *   &lt;bean id="specialService" class="a.b.c.SpecialService" singleton="false"/>
 *   &lt;bean id="anotherService" class="a.b.c.AnotherService" singleton="false"/>
 *
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean">
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/>
 *   &lt;/bean>
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean">
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/>
 *   &lt;/bean>
 *
 *&lt;/beans></pre>
 *
 * <p>随之而来的{@code MyClientBean}类实现可能看起来像这样:
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("specialService");
 *        // use the service object to effect the business logic...
 *    }
 *
 *    public void anotherBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("anotherService");
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>See {@link ObjectFactoryCreatingFactoryBean} for an alternate approach.
 */
public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

	private Class<?> serviceLocatorInterface;

	private Constructor<Exception> serviceLocatorExceptionConstructor;

	private Properties serviceMappings;

	private ListableBeanFactory beanFactory;

	private Object proxy;


	/**
	 * 设置要使用的服务定位器接口, 该接口必须具有一个或多个带有签名 {@code MyType xxx()}或{@code MyType xxx(MyIdType id)}的方法
	 * (通常是{@code MyService getService()}或{@code MyService getService(String id)}).
	 * 有关此类方法语义的信息, 请参阅 {@link ServiceLocatorFactoryBean class-level Javadoc}.
	 */
	public void setServiceLocatorInterface(Class<?> interfaceType) {
		this.serviceLocatorInterface = interfaceType;
	}

	/**
	 * 设置服务定位器在服务查找失败时应抛出的异常类.
	 * 指定的异常类必须具有带以下参数类型之一的构造函数:
	 * {@code (String, Throwable)} or {@code (Throwable)} or {@code (String)}.
	 * <p>如果未指定, 将抛出Spring的BeansException的子类, 例如NoSuchBeanDefinitionException.
	 * 由于这些是未经检查的, 调用者不需要处理它们, 因此只要它们被一般地处理就可以接受抛出Spring异常.
	 */
	public void setServiceLocatorExceptionClass(Class<? extends Exception> serviceLocatorExceptionClass) {
		if (serviceLocatorExceptionClass != null && !Exception.class.isAssignableFrom(serviceLocatorExceptionClass)) {
			throw new IllegalArgumentException(
					"serviceLocatorException [" + serviceLocatorExceptionClass.getName() + "] is not a subclass of Exception");
		}
		this.serviceLocatorExceptionConstructor =
				determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
	}

	/**
	 * 设置服务ID(传递到服务定位器)和bean名称(在bean工厂中)之间的映射.
	 * 未在此处定义的服务ID将被视为原样的bean名称.
	 * <p>作为服务id键的空字符串定义{@code null}和空字符串的映射, 以及不带参数的工厂方法.
	 * 如果未定义, 将从bean工厂检索单个匹配的bean.
	 * 
	 * @param serviceMappings 服务ID和bean名称之间的映射, 服务ID作为Key, bean名称作为值
	 */
	public void setServiceMappings(Properties serviceMappings) {
		this.serviceMappings = serviceMappings;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new FatalBeanException(
					"ServiceLocatorFactoryBean needs to run in a BeanFactory that is a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.serviceLocatorInterface == null) {
			throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
		}

		// Create service locator proxy.
		this.proxy = Proxy.newProxyInstance(
				this.serviceLocatorInterface.getClassLoader(),
				new Class<?>[] {this.serviceLocatorInterface},
				new ServiceLocatorInvocationHandler());
	}


	/**
	 * 确定用于给定服务定位器异常类的构造函数. 仅在自定义服务定位器异常的情况下调用.
	 * <p>默认实现查找具有以下参数类型之一的构造函数:
	 * {@code (String, Throwable)} or {@code (Throwable)} or {@code (String)}.
	 * 
	 * @param exceptionClass 异常类
	 * 
	 * @return 要使用的构造函数
	 */
	@SuppressWarnings("unchecked")
	protected Constructor<Exception> determineServiceLocatorExceptionConstructor(Class<? extends Exception> exceptionClass) {
		try {
			return (Constructor<Exception>) exceptionClass.getConstructor(String.class, Throwable.class);
		}
		catch (NoSuchMethodException ex) {
			try {
				return (Constructor<Exception>) exceptionClass.getConstructor(Throwable.class);
			}
			catch (NoSuchMethodException ex2) {
				try {
					return (Constructor<Exception>) exceptionClass.getConstructor(String.class);
				}
				catch (NoSuchMethodException ex3) {
					throw new IllegalArgumentException(
							"Service locator exception [" + exceptionClass.getName() +
							"] neither has a (String, Throwable) constructor nor a (String) constructor");
				}
			}
		}
	}

	/**
	 * 为给定原因创建服务定位器异常.
	 * 仅在自定义服务定位器异常的情况下调用.
	 * <p>默认实现可以处理消息和异常参数的所有变体.
	 * 
	 * @param exceptionConstructor 要使用的构造函数
	 * @param cause 服务查找失败的原因
	 * 
	 * @return 要抛出的服务定位器异常
	 */
	protected Exception createServiceLocatorException(Constructor<Exception> exceptionConstructor, BeansException cause) {
		Class<?>[] paramTypes = exceptionConstructor.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (String.class == paramTypes[i]) {
				args[i] = cause.getMessage();
			}
			else if (paramTypes[i].isInstance(cause)) {
				args[i] = cause;
			}
		}
		return BeanUtils.instantiateClass(exceptionConstructor, args);
	}


	@Override
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceLocatorInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 将服务定位器调用委托给bean工厂的调用处理程序.
	 */
	private class ServiceLocatorInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// Use hashCode of service locator proxy.
				return System.identityHashCode(proxy);
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "Service locator: " + serviceLocatorInterface;
			}
			else {
				return invokeServiceLocatorMethod(method, args);
			}
		}

		private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
			Class<?> serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
			try {
				String beanName = tryGetBeanName(args);
				if (StringUtils.hasLength(beanName)) {
					// Service locator for a specific bean name
					return beanFactory.getBean(beanName, serviceLocatorMethodReturnType);
				}
				else {
					// Service locator for a bean type
					return beanFactory.getBean(serviceLocatorMethodReturnType);
				}
			}
			catch (BeansException ex) {
				if (serviceLocatorExceptionConstructor != null) {
					throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
				}
				throw ex;
			}
		}

		/**
		 * 检查是否传入了服务ID.
		 */
		private String tryGetBeanName(Object[] args) {
			String beanName = "";
			if (args != null && args.length == 1 && args[0] != null) {
				beanName = args[0].toString();
			}
			// Look for explicit serviceId-to-beanName mappings.
			if (serviceMappings != null) {
				String mappedName = serviceMappings.getProperty(beanName);
				if (mappedName != null) {
					beanName = mappedName;
				}
			}
			return beanName;
		}

		private Class<?> getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
			Class<?>[] paramTypes = method.getParameterTypes();
			Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
			Class<?> serviceLocatorReturnType = interfaceMethod.getReturnType();

			// Check whether the method is a valid service locator.
			if (paramTypes.length > 1 || void.class == serviceLocatorReturnType) {
				throw new UnsupportedOperationException(
						"May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " +
						"on factory interface, but tried to call: " + interfaceMethod);
			}
			return serviceLocatorReturnType;
		}
	}

}
