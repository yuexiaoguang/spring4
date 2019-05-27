package org.springframework.context.event;

import java.lang.reflect.Constructor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * 在每次成功调用方法后, 向{@code ApplicationEventPublisher}注册的所有{@code ApplicationListeners}发布{@code ApplicationEvent}.
 *
 * <p>请注意, 此拦截器只能发布通过{@link #setApplicationEventClass "applicationEventClass"}属性配置的<i>无状态</i>事件.
 */
public class EventPublicationInterceptor
		implements MethodInterceptor, ApplicationEventPublisherAware, InitializingBean {

	private Constructor<?> applicationEventClassConstructor;

	private ApplicationEventPublisher applicationEventPublisher;


	/**
	 * 设置要发布的应用程序事件类.
	 * <p>事件类<b>必须</b>具有一个构造函数, 该构造函数具有事件源的单个{@code Object}参数.
	 * 拦截器将传入调用的对象.
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code Class}是{@code null};
	 * 或者它不是{@code ApplicationEvent}子类; 或者它没有公开一个只接受一个{@code Object}参数构造函数
	 */
	public void setApplicationEventClass(Class<?> applicationEventClass) {
		if (ApplicationEvent.class == applicationEventClass ||
				!ApplicationEvent.class.isAssignableFrom(applicationEventClass)) {
			throw new IllegalArgumentException("'applicationEventClass' needs to extend ApplicationEvent");
		}
		try {
			this.applicationEventClassConstructor = applicationEventClass.getConstructor(Object.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("ApplicationEvent class [" +
					applicationEventClass.getName() + "] does not have the required Object constructor: " + ex);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.applicationEventClassConstructor == null) {
			throw new IllegalArgumentException("Property 'applicationEventClass' is required");
		}
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object retVal = invocation.proceed();

		ApplicationEvent event = (ApplicationEvent)
				this.applicationEventClassConstructor.newInstance(invocation.getThis());
		this.applicationEventPublisher.publishEvent(event);

		return retVal;
	}

}
