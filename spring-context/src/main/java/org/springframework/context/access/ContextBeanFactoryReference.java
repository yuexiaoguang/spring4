package org.springframework.context.access;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 特定ApplicationContext的 BeanFactoryReference实现, 包装新创建的ApplicationContext, 在释放时关闭它.
 *
 * <p>根据 BeanFactoryReference约定, {@code release}可能会被多次调用, 后续调用没有做任何事情.
 * 但是, 在{@code release}调用之后调用{@code getFactory}将导致异常.
 */
public class ContextBeanFactoryReference implements BeanFactoryReference {

	private ApplicationContext applicationContext;


	/**
	 * @param applicationContext 要包装的ApplicationContext
	 */
	public ContextBeanFactoryReference(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	public BeanFactory getFactory() {
		if (this.applicationContext == null) {
			throw new IllegalStateException(
					"ApplicationContext owned by this BeanFactoryReference has been released");
		}
		return this.applicationContext;
	}

	@Override
	public void release() {
		if (this.applicationContext != null) {
			ApplicationContext savedCtx;

			// 实际上并不保证线程安全, 但这不是额外的工作.
			synchronized (this) {
				savedCtx = this.applicationContext;
				this.applicationContext = null;
			}

			if (savedCtx != null && savedCtx instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) savedCtx).close();
			}
		}
	}

}
