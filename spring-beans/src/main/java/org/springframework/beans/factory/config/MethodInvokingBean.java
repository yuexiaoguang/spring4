package org.springframework.beans.factory.config;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.ClassUtils;

/**
 * 简单的方法调用bean: 只是调用一个目标方法, 而不是期望结果暴露给容器 (相对于{@link MethodInvokingFactoryBean}).
 *
 * <p>这个调用者支持任何类型的目标方法. 可以通过将{@link #setTargetMethod targetMethod}属性设置为表示静态方法名称的String来指定静态方法,
 * 使用{@link #setTargetClass targetClass}指定定义静态方法的Class.
 * 或者, 可以通过将{@link #setTargetObject targetObject}属性设置为目标对象来指定目标实例方法,
 * 以及{@link #setTargetMethod targetMethod}属性作为要在该目标对象上调用的方法的名称.
 * 可以通过设置{@link #setArguments arguments}属性来指定方法调用的参数.
 *
 * <p>根据InitializingBean契约, 一旦设置了所有属性, 此类依赖于{@link #afterPropertiesSet()}被调用.
 *
 * <p>bean定义的一个示例(在基于XML的bean工厂定义中), 它使用此类来调用静态初始化方法:
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingBean">
 *   &lt;property name="staticMethod" value="com.whatever.MyClass.init"/>
 * &lt;/bean></pre>
 *
 * <p>调用实例方法以启动某个服务器bean的示例:
 *
 * <pre class="code">
 * &lt;bean id="myStarter" class="org.springframework.beans.factory.config.MethodInvokingBean">
 *   &lt;property name="targetObject" ref="myServer"/>
 *   &lt;property name="targetMethod" value="start"/>
 * &lt;/bean></pre>
 */
public class MethodInvokingBean extends ArgumentConvertingMethodInvoker
		implements BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	/**
	 * 从Bean运行的BeanFactory中获取TypeConverter.
	 */
	@Override
	protected TypeConverter getDefaultTypeConverter() {
		if (this.beanFactory != null) {
			return this.beanFactory.getTypeConverter();
		}
		else {
			return super.getDefaultTypeConverter();
		}
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		invokeWithTargetException();
	}

	/**
	 * 执行调用, 并将InvocationTargetException转换为底层目标异常.
	 */
	protected Object invokeWithTargetException() throws Exception {
		try {
			return invoke();
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof Exception) {
				throw (Exception) ex.getTargetException();
			}
			if (ex.getTargetException() instanceof Error) {
				throw (Error) ex.getTargetException();
			}
			throw ex;
		}
	}

}
