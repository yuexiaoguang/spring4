package org.springframework.messaging.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * 消息模板, 用于将目标名称解析为{@link MessageChannel}以发送和接收消息.
 */
public class GenericMessagingTemplate extends AbstractDestinationResolvingMessagingTemplate<MessageChannel>
		implements BeanFactoryAware {

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	private volatile boolean throwExceptionOnLateReply = false;


	/**
	 * 配置用于发送操作的超时值.
	 * 
	 * @param sendTimeout 发送超时, 以毫秒为单位
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * 返回配置的发送操作超时值.
	 */
	public long getSendTimeout() {
		return this.sendTimeout;
	}

	/**
	 * 配置用于接收操作的超时值.
	 * 
	 * @param receiveTimeout 接收超时, 以毫秒为单位
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回配置的接收操作超时值.
	 */
	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	/**
	 * 发送回复的线程是否应该引发异常, 如果接收线程由于超时而未收到回复,
	 * 或者因为它已经收到回复, 或者因为它在发送请求消息时出现异常.
	 * <p>默认值为{@code false}, 在这种情况下, 仅记录WARN消息.
	 * 如果设置为{@code true}, 则除了日志消息之外还会引发{@link MessageDeliveryException}.
	 * 
	 * @param throwExceptionOnLateReply 是否抛出异常
	 */
	public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
		this.throwExceptionOnLateReply = throwExceptionOnLateReply;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		setDestinationResolver(new BeanFactoryMessageChannelDestinationResolver(beanFactory));
	}


	@Override
	protected final void doSend(MessageChannel channel, Message<?> message) {
		Assert.notNull(channel, "MessageChannel is required");

		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor != null && accessor.isMutable()) {
			accessor.setImmutable();
		}

		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0 ? channel.send(message, timeout) : channel.send(message));

		if (!sent) {
			throw new MessageDeliveryException(message,
					"Failed to send message to channel '" + channel + "' within timeout: " + timeout);
		}
	}

	@Override
	protected final Message<?> doReceive(MessageChannel channel) {
		Assert.notNull(channel, "MessageChannel is required");
		Assert.state(channel instanceof PollableChannel, "A PollableChannel is required to receive messages");

		long timeout = this.receiveTimeout;
		Message<?> message = (timeout >= 0 ?
				((PollableChannel) channel).receive(timeout) : ((PollableChannel) channel).receive());

		if (message == null && this.logger.isTraceEnabled()) {
			this.logger.trace("Failed to receive message from channel '" + channel + "' within timeout: " + timeout);
		}

		return message;
	}

	@Override
	protected final Message<?> doSendAndReceive(MessageChannel channel, Message<?> requestMessage) {
		Assert.notNull(channel, "'channel' is required");
		Object originalReplyChannelHeader = requestMessage.getHeaders().getReplyChannel();
		Object originalErrorChannelHeader = requestMessage.getHeaders().getErrorChannel();

		TemporaryReplyChannel tempReplyChannel = new TemporaryReplyChannel();
		requestMessage = MessageBuilder.fromMessage(requestMessage).setReplyChannel(tempReplyChannel).
				setErrorChannel(tempReplyChannel).build();

		try {
			doSend(channel, requestMessage);
		}
		catch (RuntimeException ex) {
			tempReplyChannel.setSendFailed(true);
			throw ex;
		}

		Message<?> replyMessage = this.doReceive(tempReplyChannel);
		if (replyMessage != null) {
			replyMessage = MessageBuilder.fromMessage(replyMessage)
					.setHeader(MessageHeaders.REPLY_CHANNEL, originalReplyChannelHeader)
					.setHeader(MessageHeaders.ERROR_CHANNEL, originalErrorChannelHeader)
					.build();
		}

		return replyMessage;
	}


	/**
	 * 用于接收单个回复消息的临时频道.
	 */
	private class TemporaryReplyChannel implements PollableChannel {

		private final Log logger = LogFactory.getLog(TemporaryReplyChannel.class);

		private final CountDownLatch replyLatch = new CountDownLatch(1);

		private volatile Message<?> replyMessage;

		private volatile boolean hasReceived;

		private volatile boolean hasTimedOut;

		private volatile boolean hasSendFailed;

		public void setSendFailed(boolean hasSendError) {
			this.hasSendFailed = hasSendError;
		}

		@Override
		public Message<?> receive() {
			return this.receive(-1);
		}

		@Override
		public Message<?> receive(long timeout) {
			try {
				if (GenericMessagingTemplate.this.receiveTimeout < 0) {
					this.replyLatch.await();
					this.hasReceived = true;
				}
				else {
					if (this.replyLatch.await(GenericMessagingTemplate.this.receiveTimeout, TimeUnit.MILLISECONDS)) {
						this.hasReceived = true;
					}
					else {
						this.hasTimedOut = true;
					}
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return this.replyMessage;
		}

		@Override
		public boolean send(Message<?> message) {
			return this.send(message, -1);
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			this.replyMessage = message;
			boolean alreadyReceivedReply = this.hasReceived;
			this.replyLatch.countDown();

			String errorDescription = null;
			if (this.hasTimedOut) {
				errorDescription = "Reply message received but the receiving thread has exited due to a timeout";
			}
			else if (alreadyReceivedReply) {
				errorDescription = "Reply message received but the receiving thread has already received a reply";
			}
			else if (this.hasSendFailed) {
				errorDescription = "Reply message received but the receiving thread has exited due to " +
						"an exception while sending the request message";
			}

			if (errorDescription != null) {
				if (logger.isWarnEnabled()) {
					logger.warn(errorDescription + ":" + message);
				}
				if (GenericMessagingTemplate.this.throwExceptionOnLateReply) {
					throw new MessageDeliveryException(message, errorDescription);
				}
			}

			return true;
		}
	}
}
