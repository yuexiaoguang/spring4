package org.springframework.messaging.simp.user;

import org.springframework.messaging.Message;

/**
 * 通过将其转换为每个活动用户会话一个或多个实际目标来解析"user"目标的策略.
 * 将消息发送到用户目标时, 目标必须包含用户名, 以便可以将其提取出来并用于查找用户会话.
 * 订阅用户目标时, 目标不必包含用户自己的名称.
 * 只是使用当前会话.
 *
 * <p>请参阅实现类和示例目标的文档.
 */
public interface UserDestinationResolver {

	/**
	 * 使用用户目标将给定消息解析为具有实际目标的一个或多个消息, 每个活动用户会话一个消息.
	 * 
	 * @param message 要尝试解析的消息
	 * 
	 * @return 0个或多个目标消息 (每个活动会话一个), 或{@code null} 如果源消息不包含用户目标.
	 */
	UserDestinationResult resolveDestination(Message<?> message);

}
