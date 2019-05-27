package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * {@link MessageSendingOperations}的专业化, 以及与简单消息协议 (如 STOMP)的Spring Framework支持一起使用的方法.
 *
 * <p>有关用户目标的更多信息, 请参阅{@link org.springframework.messaging.simp.user.UserDestinationResolver UserDestinationResolver}.
 *
 * <p>通常, 期望用户是使用WebSocket会话进行身份验证的用户 (或者通过扩展使用启动会话的握手请求进行身份验证的用户).
 * 但是, 如果会话未经过身份验证, 则还可以传递会话ID (如果已知)来代替用户名.
 * 在这种情况下, 必须使用一个接受header的重载方法,
 * 以确保已相应地设置{@link org.springframework.messaging.simp.SimpMessageHeaderAccessor#setSessionId sessionId} header.
 */
public interface SimpMessageSendingOperations extends MessageSendingOperations<String> {

	/**
	 * 向给定用户发送消息.
	 * 
	 * @param user 应该接收消息的用户.
	 * @param destination 发送到的目标.
	 * @param payload 要发送的有效负载
	 */
	void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException;

	/**
	 * 向给定用户发送消息.
	 * <p>默认情况下, header被解释为本机header (e.g. STOMP),
	 * 并保存在生成​​的Spring {@link org.springframework.messaging.Message Message}中的特殊键下.
	 * 当消息离开应用程序时生效, 提供的header随其一起传送到目标 (e.g. STOMP 客户端或代理).
	 * <p>如果Map已包含键
	 * {@link org.springframework.messaging.support.NativeMessageHeaderAccessor#NATIVE_HEADERS "nativeHeaders"}
	 * 或已使用
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor SimpMessageHeaderAccessor}准备好,
	 * 则会直接使用header.
	 * 常见的预期情况是提供内容类型 (以影响消息转换) 和本机header.
	 * 这可以如下完成:
	 * <pre class="code">
	 * SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
	 * accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);
	 * accessor.setNativeHeader("foo", "bar");
	 * accessor.setLeaveMutable(true);
	 * MessageHeaders headers = accessor.getMessageHeaders();
	 * messagingTemplate.convertAndSendToUser(user, destination, payload, headers);
	 * </pre>
	 * <p><strong>Note:</strong> 如果{@code MessageHeaders}是可变的, 如上例所示,
	 * 此接口的实现应注意并更新同一实例中的header (而不是复制或重新创建), 然后在发送最终消息之前将其设置为不可变的.
	 * 
	 * @param user 应该接收消息的用户 (must not be {@code null})
	 * @param destination 目标 (must not be {@code null})
	 * @param payload 要发送的有效负载 (may be {@code null})
	 * @param headers 消息header (may be {@code null})
	 */
	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 向给定用户发送消息.
	 * 
	 * @param user 应该接收消息的用户 (must not be {@code null})
	 * @param destination 目标 (must not be {@code null})
	 * @param payload 要发送的有效负载 (may be {@code null})
	 * @param postProcessor 后处理或修改创建的消息
	 */
	void convertAndSendToUser(String user, String destination, Object payload,
			MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 向给定用户发送消息.
	 * <p>有关输入header的重要说明, 请参阅{@link #convertAndSend(Object, Object, java.util.Map)}.
	 * 
	 * @param user 应该接收消息的用户
	 * @param destination 目标
	 * @param payload 要发送的有效负载
	 * @param headers 消息header
	 * @param postProcessor 后处理或修改创建的消息
	 */
	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException;

}
