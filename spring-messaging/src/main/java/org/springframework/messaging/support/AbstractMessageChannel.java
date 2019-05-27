package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link MessageChannel}实现的抽象基类.
 */
public abstract class AbstractMessageChannel implements MessageChannel, InterceptableChannel, BeanNameAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>(5);

	private String beanName;


	public AbstractMessageChannel() {
		this.beanName = getClass().getSimpleName() + "@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * 消息通道主要使用bean名称进行日志记录.
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * 返回此消息通道的bean名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}


	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors.clear();
		this.interceptors.addAll(interceptors);
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		this.interceptors.add(index, interceptor);
	}

	@Override
	public List<ChannelInterceptor> getInterceptors() {
		return Collections.unmodifiableList(this.interceptors);
	}

	@Override
	public boolean removeInterceptor(ChannelInterceptor interceptor) {
		return this.interceptors.remove(interceptor);
	}

	@Override
	public ChannelInterceptor removeInterceptor(int index) {
		return this.interceptors.remove(index);
	}


	@Override
	public final boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	@Override
	public final boolean send(Message<?> message, long timeout) {
		Assert.notNull(message, "Message must not be null");
		ChannelInterceptorChain chain = new ChannelInterceptorChain();
		boolean sent = false;
		try {
			message = chain.applyPreSend(message, this);
			if (message == null) {
				return false;
			}
			sent = sendInternal(message, timeout);
			chain.applyPostSend(message, this, sent);
			chain.triggerAfterSendCompletion(message, this, sent, null);
			return sent;
		}
		catch (Exception ex) {
			chain.triggerAfterSendCompletion(message, this, sent, ex);
			if (ex instanceof MessagingException) {
				throw (MessagingException) ex;
			}
			throw new MessageDeliveryException(message,"Failed to send message to " + this, ex);
		}
		catch (Throwable err) {
			MessageDeliveryException ex2 =
					new MessageDeliveryException(message, "Failed to send message to " + this, err);
			chain.triggerAfterSendCompletion(message, this, sent, ex2);
			throw ex2;
		}
	}

	protected abstract boolean sendInternal(Message<?> message, long timeout);


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.beanName + "]";
	}


	/**
	 * 协助调用已配置的通道拦截器.
	 */
	protected class ChannelInterceptorChain {

		private int sendInterceptorIndex = -1;

		private int receiveInterceptorIndex = -1;

		public Message<?> applyPreSend(Message<?> message, MessageChannel channel) {
			Message<?> messageToUse = message;
			for (ChannelInterceptor interceptor : interceptors) {
				Message<?> resolvedMessage = interceptor.preSend(messageToUse, channel);
				if (resolvedMessage == null) {
					String name = interceptor.getClass().getSimpleName();
					if (logger.isDebugEnabled()) {
						logger.debug(name + " returned null from preSend, i.e. precluding the send.");
					}
					triggerAfterSendCompletion(messageToUse, channel, false, null);
					return null;
				}
				messageToUse = resolvedMessage;
				this.sendInterceptorIndex++;
			}
			return messageToUse;
		}

		public void applyPostSend(Message<?> message, MessageChannel channel, boolean sent) {
			for (ChannelInterceptor interceptor : interceptors) {
				interceptor.postSend(message, channel, sent);
			}
		}

		public void triggerAfterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
			for (int i = this.sendInterceptorIndex; i >= 0; i--) {
				ChannelInterceptor interceptor = interceptors.get(i);
				try {
					interceptor.afterSendCompletion(message, channel, sent, ex);
				}
				catch (Throwable ex2) {
					logger.error("Exception from afterSendCompletion in " + interceptor, ex2);
				}
			}
		}

		public boolean applyPreReceive(MessageChannel channel) {
			for (ChannelInterceptor interceptor : interceptors) {
				if (!interceptor.preReceive(channel)) {
					triggerAfterReceiveCompletion(null, channel, null);
					return false;
				}
				this.receiveInterceptorIndex++;
			}
			return true;
		}

		public Message<?> applyPostReceive(Message<?> message, MessageChannel channel) {
			for (ChannelInterceptor interceptor : interceptors) {
				message = interceptor.postReceive(message, channel);
				if (message == null) {
					return null;
				}
			}
			return message;
		}

		public void triggerAfterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
			for (int i = this.receiveInterceptorIndex; i >= 0; i--) {
				ChannelInterceptor interceptor = interceptors.get(i);
				try {
					interceptor.afterReceiveCompletion(message, channel, ex);
				}
				catch (Throwable ex2) {
					if (logger.isErrorEnabled()) {
						logger.error("Exception from afterReceiveCompletion in " + interceptor, ex2);
					}
				}
			}
		}
	}

}
