package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * {@link ChannelInterceptor}的扩展, 带有回调, 通过{@link java.util.concurrent.Executor}
 * 拦截{@link org.springframework.messaging.Message}异步发送给特定订阅者.
 * 可以使用Executor配置的{@link org.springframework.messaging.MessageChannel}实现支持.
 */
public interface ExecutorChannelInterceptor extends ChannelInterceptor {

	/**
	 * 在调用目标MessageHandler处理消息之前, 在提交给Executor的{@link Runnable}内部调用.
	 * 允许在必要时修改消息, 或者在返回{@code null}时不调用MessageHandler.
	 * 
	 * @param message 要处理的消息
	 * @param channel 消息发送到的通道
	 * @param handler 处理消息的目标处理器
	 * 
	 * @return 输入消息, 或新实例, 或{@code null}
	 */
	Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler);

	/**
	 * 在调用目标MessageHandler之后, 在提交给Executor的{@link Runnable}的内部调用,
	 * 无论结果如何(i.e. 是否有异常), 从而允许适当的资源清理.
	 * <p>请注意, 仅当beforeHandle成功完成并返回Message时才会调用此方法, i.e. 它不会返回{@code null}.
	 * 
	 * @param message 要处理的消息
	 * @param channel 消息发送到的通道
	 * @param handler 处理消息的目标处理器
	 * @param ex 处理器可能引发的任何异常
	 */
	void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex);

}
