package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

/**
 * 一个{@link SubscribableChannel}, 它向每个订阅者发送消息.
 */
public class ExecutorSubscribableChannel extends AbstractSubscribableChannel {

	private final Executor executor;

	private final List<ExecutorChannelInterceptor> executorInterceptors = new ArrayList<ExecutorChannelInterceptor>(4);


	/**
	 * 消息将在调用者线程中发送.
	 */
	public ExecutorSubscribableChannel() {
		this(null);
	}

	/**
	 * 消息将通过指定的执行器发送.
	 * 
	 * @param executor 用于发送消息的执行器, 或{@code null}以在调用者线程中执行.
	 */
	public ExecutorSubscribableChannel(Executor executor) {
		this.executor = executor;
	}


	public Executor getExecutor() {
		return this.executor;
	}

	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		super.setInterceptors(interceptors);
		this.executorInterceptors.clear();
		for (ChannelInterceptor interceptor : interceptors) {
			if (interceptor instanceof ExecutorChannelInterceptor) {
				this.executorInterceptors.add((ExecutorChannelInterceptor) interceptor);
			}
		}
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		super.addInterceptor(interceptor);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptors.add((ExecutorChannelInterceptor) interceptor);
		}
	}


	@Override
	public boolean sendInternal(Message<?> message, long timeout) {
		for (MessageHandler handler : getSubscribers()) {
			SendTask sendTask = new SendTask(message, handler);
			if (this.executor == null) {
				sendTask.run();
			}
			else {
				this.executor.execute(sendTask);
			}
		}
		return true;
	}


	/**
	 * 使用ExecutorChannelInterceptors调用MessageHandler.
	 */
	private class SendTask implements MessageHandlingRunnable {

		private final Message<?> inputMessage;

		private final MessageHandler messageHandler;

		private int interceptorIndex = -1;

		public SendTask(Message<?> message, MessageHandler messageHandler) {
			this.inputMessage = message;
			this.messageHandler = messageHandler;
		}

		@Override
		public Message<?> getMessage() {
			return this.inputMessage;
		}

		@Override
		public MessageHandler getMessageHandler() {
			return this.messageHandler;
		}

		@Override
		public void run() {
			Message<?> message = this.inputMessage;
			try {
				message = applyBeforeHandle(message);
				if (message == null) {
					return;
				}
				this.messageHandler.handleMessage(message);
				triggerAfterMessageHandled(message, null);
			}
			catch (Exception ex) {
				triggerAfterMessageHandled(message, ex);
				if (ex instanceof MessagingException) {
					throw (MessagingException) ex;
				}
				String description = "Failed to handle " + message + " to " + this + " in " + this.messageHandler;
				throw new MessageDeliveryException(message, description, ex);
			}
			catch (Throwable err) {
				String description = "Failed to handle " + message + " to " + this + " in " + this.messageHandler;
				MessageDeliveryException ex2 = new MessageDeliveryException(message, description, err);
				triggerAfterMessageHandled(message, ex2);
				throw ex2;
			}
		}

		private Message<?> applyBeforeHandle(Message<?> message) {
			Message<?> messageToUse = message;
			for (ExecutorChannelInterceptor interceptor : executorInterceptors) {
				messageToUse = interceptor.beforeHandle(messageToUse, ExecutorSubscribableChannel.this, this.messageHandler);
				if (messageToUse == null) {
					String name = interceptor.getClass().getSimpleName();
					if (logger.isDebugEnabled()) {
						logger.debug(name + " returned null from beforeHandle, i.e. precluding the send.");
					}
					triggerAfterMessageHandled(message, null);
					return null;
				}
				this.interceptorIndex++;
			}
			return messageToUse;
		}

		private void triggerAfterMessageHandled(Message<?> message, Exception ex) {
			for (int i = this.interceptorIndex; i >= 0; i--) {
				ExecutorChannelInterceptor interceptor = executorInterceptors.get(i);
				try {
					interceptor.afterMessageHandled(message, ExecutorSubscribableChannel.this, this.messageHandler, ex);
				}
				catch (Throwable ex2) {
					logger.error("Exception from afterMessageHandled in " + interceptor, ex2);
				}
			}
		}
	}

}
