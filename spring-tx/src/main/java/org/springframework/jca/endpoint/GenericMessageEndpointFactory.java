package org.springframework.jca.endpoint;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.util.ReflectionUtils;

/**
 * JCA 1.5 {@link javax.resource.spi.endpoint.MessageEndpointFactory}接口的通用实现,
 * 为任何类型的消息监听器对象提供事务管理功能 (e.g. {@link javax.jms.MessageListener}对象或{@link javax.resource.cci.MessageListener}对象).
 *
 * <p>对具体端点实例使用AOP代理, 只需包装指定的消息监听器对象, 并在端点实例上公开其所有已实现的接口.
 *
 * <p>通常与Spring的{@link GenericMessageEndpointManager}一起使用, 但不与它绑定.
 * 因此, 此端点工厂也可用于本机{@link javax.resource.spi.ResourceAdapter}实例上的编程端点管理.
 */
public class GenericMessageEndpointFactory extends AbstractMessageEndpointFactory {

	private Object messageListener;


	/**
	 * 指定端点应公开的消息监听器对象
	 * (e.g. {@link javax.jms.MessageListener}对象或{@link javax.resource.cci.MessageListener}实现).
	 */
	public void setMessageListener(Object messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * 使用AOP代理包装每个具体端点实例, 通过AOP公开消息监听器的接口以及端点SPI.
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		GenericMessageEndpoint endpoint = (GenericMessageEndpoint) super.createEndpoint(xaResource);
		ProxyFactory proxyFactory = new ProxyFactory(this.messageListener);
		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(endpoint);
		introduction.suppressInterface(MethodInterceptor.class);
		proxyFactory.addAdvice(introduction);
		return (MessageEndpoint) proxyFactory.getProxy();
	}

	/**
	 * 创建此工厂内部的具体通用消息端点.
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new GenericMessageEndpoint();
	}


	/**
	 * 实现具体通用消息端点的私有内部类, 作为将由代理调用的AOP Alliance MethodInterceptor.
	 */
	private class GenericMessageEndpoint extends AbstractMessageEndpoint implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			Throwable endpointEx = null;
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw adaptExceptionIfNecessary(methodInvocation, ex);
				}
			}
			try {
				return methodInvocation.proceed();
			}
			catch (Throwable ex) {
				endpointEx = ex;
				onEndpointException(ex);
				throw ex;
			}
			finally {
				if (applyDeliveryCalls) {
					try {
						afterDelivery();
					}
					catch (ResourceException ex) {
						if (endpointEx == null) {
							throw adaptExceptionIfNecessary(methodInvocation, ex);
						}
					}
				}
			}
		}

		private Exception adaptExceptionIfNecessary(MethodInvocation methodInvocation, ResourceException ex) {
			if (ReflectionUtils.declaresException(methodInvocation.getMethod(), ex.getClass())) {
				return ex;
			}
			else {
				return new InternalResourceException(ex);
			}
		}

		@Override
		protected ClassLoader getEndpointClassLoader() {
			return messageListener.getClass().getClassLoader();
		}
	}


	/**
	 * 在端点调用期间遇到ResourceException时抛出的内部异常.
	 * <p>仅当ResourceAdapter不直接调用端点的{@code beforeDelivery} 和{@code afterDelivery},
	 * 将其留给具体端点以应用它们 - 并处理从它们抛出的任何ResourceExceptions时, 才会使用.
	 */
	@SuppressWarnings("serial")
	public static class InternalResourceException extends RuntimeException {

		protected InternalResourceException(ResourceException cause) {
			super(cause);
		}
	}
}
