package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * 拦截器的接口，能够查看和/或修改从{@link MessageChannel}发送和/或接收的{@link Message Messages}.
 */
public interface ChannelInterceptor {

	/**
	 * 在消息实际发送到通道之前调用.
	 * 允许修改消息.
	 * 如果此方法返回{@code null}, 则不会发生实际的发送调用.
	 */
	Message<?> preSend(Message<?> message, MessageChannel channel);

	/**
	 * 在发送调用后立即调用.
	 * boolean 参数表示该调用的返回值.
	 */
	void postSend(Message<?> message, MessageChannel channel, boolean sent);

	/**
	 * 在发送完成后调用, 无论是否已经引发任何异常, 从而允许适当的资源清理.
	 * <p>请注意, 仅当{@link #preSend} 成功完成并返回消息时才会调用此方法, i.e. 它不会返回{@code null}.
	 */
	void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex);

	/**
	 * 调用receive后立即调用, 并在实际检索消息之前调用.
	 * 如果返回值为'false', 则不会检索任何消息. 这仅适用于 PollableChannel.
	 */
	boolean preReceive(MessageChannel channel);

	/**
	 * 在检索到Message之后但在将其返回给调用者之前立即调用.
	 * 可以修改消息. 这仅适用于 PollableChannels.
	 */
	Message<?> postReceive(Message<?> message, MessageChannel channel);

	/**
	 * 在完成接收后调用, 无论是否已经引发任何异常, 从而允许适当的资源清理.
	 * <p>请注意, 仅当{@link #preReceive}成功完成并返回{@code true}时才会调用此方法.
	 */
	void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex);

}
