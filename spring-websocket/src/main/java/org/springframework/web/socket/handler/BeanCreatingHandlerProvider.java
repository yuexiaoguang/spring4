package org.springframework.web.socket.handler;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * 通过Spring {@link BeanFactory}实例化目标处理器, 并提供等效的destroy方法.
 * 主要用于内部使用, 以协助初始化和销毁​​每个连接生命周期的处理器.
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
