package org.springframework.remoting.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.ClassUtils;

/**
 * 远程访问器和导出器的通用支持基类, 提供常见的bean ClassLoader处理.
 */
public abstract class RemotingSupport implements BeanClassLoaderAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回此访问器使用的ClassLoader, 用于反序列化和生成代理.
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	/**
	 * 如有必要, 用环境的bean ClassLoader覆盖线程上下文ClassLoader,
	 * i.e. 如果bean ClassLoader已经不等同于线程上下文ClassLoader.
	 * 
	 * @return 原始线程上下文ClassLoader, 或{@code null}如果未重写
	 */
	protected ClassLoader overrideThreadContextClassLoader() {
		return ClassUtils.overrideThreadContextClassLoader(getBeanClassLoader());
	}

	/**
	 * 如有必要, 重置原始线程上下文ClassLoader.
	 * 
	 * @param original 原始线程上下文ClassLoader; 如果没有重写, 则为{@code null} (因此无需重置)
	 */
	protected void resetThreadContextClassLoader(ClassLoader original) {
		if (original != null) {
			Thread.currentThread().setContextClassLoader(original);
		}
	}
}
