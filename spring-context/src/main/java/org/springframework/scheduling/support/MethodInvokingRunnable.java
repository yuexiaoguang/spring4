package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.ClassUtils;

/**
 * 实现{@link Runnable}接口的适配器, 作为基于Spring的MethodInvoker的可配置方法调用.
 *
 * <p>从{@link org.springframework.util.MethodInvoker}继承常用配置属性.
 */
public class MethodInvokingRunnable extends ArgumentConvertingMethodInvoker
		implements Runnable, BeanClassLoaderAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();
	}


	@Override
	public void run() {
		try {
			invoke();
		}
		catch (InvocationTargetException ex) {
			logger.error(getInvocationFailureMessage(), ex.getTargetException());
			// Do not throw exception, else the main loop of the scheduler might stop!
		}
		catch (Throwable ex) {
			logger.error(getInvocationFailureMessage(), ex);
			// Do not throw exception, else the main loop of the scheduler might stop!
		}
	}

	/**
	 * 为调用失败异常构建消息.
	 * 
	 * @return 错误消息, 包括目标方法名称等
	 */
	protected String getInvocationFailureMessage() {
		return "Invocation of method '" + getTargetMethod() +
				"' on target class [" + getTargetClass() + "] failed";
	}

}
