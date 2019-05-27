package org.springframework.messaging.simp;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 用于在支持基本消息模式的简单消息协议中处理消息header的基类.
 * 提供对跨协议通用的特定值的统一访问, 例如目标, 消息类型 (e.g. 发布, 订阅等), 会话ID和其他.
 *
 * <p>使用此类中的一个静态工厂方法, 然后调用getter和setter, 最后在必要时调用{@link #toMap()}以获取更新的header.
 */
public class SimpMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	private static final IdTimestampMessageHeaderInitializer headerInitializer;

	static {
		headerInitializer = new IdTimestampMessageHeaderInitializer();
		headerInitializer.setDisableIdGeneration();
		headerInitializer.setEnableTimestamp(false);
	}

	// SiMP header names

	public static final String DESTINATION_HEADER = "simpDestination";

	public static final String MESSAGE_TYPE_HEADER = "simpMessageType";

	public static final String SESSION_ID_HEADER = "simpSessionId";

	public static final String SESSION_ATTRIBUTES = "simpSessionAttributes";

	public static final String SUBSCRIPTION_ID_HEADER = "simpSubscriptionId";

	public static final String USER_HEADER = "simpUser";

	public static final String CONNECT_MESSAGE_HEADER = "simpConnectMessage";

	public static final String DISCONNECT_MESSAGE_HEADER = "simpDisconnectMessage";

	public static final String HEART_BEAT_HEADER = "simpHeartbeat";


	/**
	 * 内部使用的header, 用于"user"目标, 需要在向客户端发送消息之前恢复目标.
	 */
	public static final String ORIGINAL_DESTINATION = "simpOrigDestination";

	/**
	 * header, 向代理指示发送者将忽略错误.
	 * 只需检查header是否存在.
	 */
	public static final String IGNORE_ERROR = "simpIgnoreError";


	/**
	 * 用于创建新消息header.
	 * 此构造函数是受保护的. 请参阅此类及其子类中的工厂方法.
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType, Map<String, List<String>> externalSourceHeaders) {
		super(externalSourceHeaders);
		Assert.notNull(messageType, "MessageType must not be null");
		setHeader(MESSAGE_TYPE_HEADER, messageType);
		headerInitializer.initHeaders(this);
	}

	/**
	 * 用于访问和修改现有消息header.
	 * 此构造函数是受保护的. 请参阅此类及其子类中的工厂方法.
	 */
	protected SimpMessageHeaderAccessor(Message<?> message) {
		super(message);
		headerInitializer.initHeaders(this);
	}


	@Override
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return wrap(message);
	}

	public void setMessageTypeIfNotSet(SimpMessageType messageType) {
		if (getMessageType() == null) {
			setHeader(MESSAGE_TYPE_HEADER, messageType);
		}
	}

	public SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE_HEADER);
	}

	public void setDestination(String destination) {
		Assert.notNull(destination, "Destination must not be null");
		setHeader(DESTINATION_HEADER, destination);
	}

	public String getDestination() {
		return (String) getHeader(DESTINATION_HEADER);
	}

	public void setSubscriptionId(String subscriptionId) {
		setHeader(SUBSCRIPTION_ID_HEADER, subscriptionId);
	}

	public String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID_HEADER);
	}

	public void setSessionId(String sessionId) {
		setHeader(SESSION_ID_HEADER, sessionId);
	}

	/**
	 * @return 当前会话的ID
	 */
	public String getSessionId() {
		return (String) getHeader(SESSION_ID_HEADER);
	}

	/**
	 * 访问会话属性header的静态替代方法.
	 */
	public void setSessionAttributes(Map<String, Object> attributes) {
		setHeader(SESSION_ATTRIBUTES, attributes);
	}

	/**
	 * 返回与当前会话关联的属性.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getSessionAttributes() {
		return (Map<String, Object>) getHeader(SESSION_ATTRIBUTES);
	}

	public void setUser(Principal principal) {
		setHeader(USER_HEADER, principal);
	}

	/**
	 * 返回与当前会话关联的用户.
	 */
	public Principal getUser() {
		return (Principal) getHeader(USER_HEADER);
	}

	@Override
	public String getShortLogMessage(Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes[").append(getSessionAttributes().size()).append("]");
		}
		sb.append(getShortPayloadLogMessage(payload));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getDetailedLogMessage(Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes=").append(getSessionAttributes());
		}
		if (!CollectionUtils.isEmpty((Map<String, List<String>>) getHeader(NATIVE_HEADERS))) {
			sb.append(" nativeHeaders=").append(getHeader(NATIVE_HEADERS));
		}
		sb.append(getDetailedPayloadLogMessage(payload));
		return sb.toString();
	}

	private StringBuilder getBaseLogMessage() {
		StringBuilder sb = new StringBuilder();
		SimpMessageType messageType = getMessageType();
		sb.append(messageType != null ? messageType.name() : SimpMessageType.OTHER);
		String destination = getDestination();
		if (destination != null) {
			sb.append(" destination=").append(destination);
		}
		String subscriptionId = getSubscriptionId();
		if (subscriptionId != null) {
			sb.append(" subscriptionId=").append(subscriptionId);
		}
		sb.append(" session=").append(getSessionId());
		Principal user = getUser();
		if (user != null) {
			sb.append(" user=").append(user.getName());
		}
		return sb;
	}


	// Static factory methods and accessors

	/**
	 * 使用{@link org.springframework.messaging.simp.SimpMessageType} {@code MESSAGE}创建实例.
	 */
	public static SimpMessageHeaderAccessor create() {
		return new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null);
	}

	/**
	 * 使用给定的{@link org.springframework.messaging.simp.SimpMessageType}创建实例.
	 */
	public static SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		return new SimpMessageHeaderAccessor(messageType, null);
	}

	/**
	 * 使用给定Message的有效负载和header创建实例.
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}

	public static SimpMessageType getMessageType(Map<String, Object> headers) {
		return (SimpMessageType) headers.get(MESSAGE_TYPE_HEADER);
	}

	public static String getDestination(Map<String, Object> headers) {
		return (String) headers.get(DESTINATION_HEADER);
	}

	public static String getSubscriptionId(Map<String, Object> headers) {
		return (String) headers.get(SUBSCRIPTION_ID_HEADER);
	}

	public static String getSessionId(Map<String, Object> headers) {
		return (String) headers.get(SESSION_ID_HEADER);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getSessionAttributes(Map<String, Object> headers) {
		return (Map<String, Object>) headers.get(SESSION_ATTRIBUTES);
	}

	public static Principal getUser(Map<String, Object> headers) {
		return (Principal) headers.get(USER_HEADER);
	}

	public static long[] getHeartbeat(Map<String, Object> headers) {
		return (long[]) headers.get(HEART_BEAT_HEADER);
	}
}
