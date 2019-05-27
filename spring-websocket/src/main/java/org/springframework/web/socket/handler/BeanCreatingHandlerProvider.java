package org.springframework.web.socket.handler;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * Instantiates a target handler through a Spring {@link BeanFactory} and also provides
 * an equivalent destroy method. Mainly for internal use to assist with initializing and
 * destroying handlers with per-connection lifecycle.
 */
public class BeanCreatingHandlerProvider<T> implements BeanFactoryAware {

	private final Class<? extends T> handlerType;

	private AutowireCapableBeanFactory beanFactory;


	public BeanCreatingHandlerProvider(Class<? extends T> handlerType) {
		Assert.notNull(handlerType, "handlerType must not be null");
		this.handlerType = handlerType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	public void destroy(T handler) {
		if (this.beanFactory != null) {
			this.beanFactory.destroyBean(handler);
		}
	}


	public Class<? extends T> getHandlerType() {
		return this.handlerType;
	}

	public T getHandler() {
		if (this.beanFactory != null) {
			return this.beanFactory.createBean(this.handlerType);
		}
		else {
			return BeanUtils.instantiate(this.handlerType);
		}
	}

	@Override
	public String toString() {
		return "BeanCreatingHandlerProvider[handlerType=" + this.handlerType + "]";
	}

}
