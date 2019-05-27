package org.springframework.jms.listener.endpoint;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;

import org.springframework.jca.endpoint.AbstractMessageEndpointFactory;

/**
 * JCA 1.5 {@link javax.resource.spi.endpoint.MessageEndpointFactory}接口的JMS特定实现,
 * 为JMS监听器对象 (e.g. a {@link javax.jms.MessageListener} 对象)提供事务管理功能.
 *
 * <p>使用静态端点实现, 只需包装指定的消息监听器对象, 并在端点实例上公开其所有已实现的接口.
 *
 * <p>通常与Spring的{@link JmsMessageEndpointManager}一起使用, 但不与它绑定.
 * 因此, 此端点工厂也可用于本机{@link javax.resource.spi.ResourceAdapter}实例上的编程端点管理.
 */
public class JmsMessageEndpointFactory extends AbstractMessageEndpointFactory  {

	private MessageListener messageListener;


	/**
	 * 设置此端点的JMS MessageListener.
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * 返回此端点的JMS MessageListener.
	 */
	protected MessageListener getMessageListener() {
		return this.messageListener;
	}

	/**
	 * 创建此工厂内部的具体JMS消息端点.
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new JmsMessageEndpoint();
	}


	/**
	 * 实现具体JMS消息端点的私有内部类.
	 */
	private class JmsMessageEndpoint extends AbstractMessageEndpoint implements MessageListener {

		@Override
		public void onMessage(Message message) {
			Throwable endpointEx = null;
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw new JmsResourceException(ex);
				}
			}
			try {
				messageListener.onMessage(message);
			}
			catch (RuntimeException ex) {
				endpointEx = ex;
				onEndpointException(ex);
				throw ex;
			}
			catch (Error err) {
				endpointEx = err;
				onEndpointException(err);
				throw err;
			}
			finally {
				if (applyDeliveryCalls) {
					try {
						afterDelivery();
					}
					catch (ResourceException ex) {
						if (endpointEx == null) {
							throw new JmsResourceException(ex);
						}
					}
				}
			}
		}

		@Override
		protected ClassLoader getEndpointClassLoader() {
			return getMessageListener().getClass().getClassLoader();
		}
	}


	/**
	 * 在端点调用期间遇到ResourceException时抛出的内部异常.
	 * <p>仅当ResourceAdapter不直接调用端点的{@code beforeDelivery}和{@code afterDelivery},
	 * 将其留给具体端点以应用它们 - 并处理从它们抛出的任何ResourceExceptions时, 才会使用.
	 */
	@SuppressWarnings("serial")
	public static class JmsResourceException extends RuntimeException {

		public JmsResourceException(ResourceException cause) {
			super(cause);
		}
	}
}
