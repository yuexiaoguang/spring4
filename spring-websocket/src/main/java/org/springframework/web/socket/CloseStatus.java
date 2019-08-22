package org.springframework.web.socket;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示WebSocket关闭状态代码和原因.
 * 1xxx范围内的状态代码由协议预定义. 可选地, 可以一起发送状态码和原因.
 *
 * <p>See <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455, Section 7.4.1
 * "Defined Status Codes"</a>.
 */
public final class CloseStatus {

	/**
	 * 1000表示正常关闭, 这意味着建立连接的目的已经实现
	 */
	public static final CloseStatus NORMAL = new CloseStatus(1000);

	/**
	 * 1001表示端点 "消失", 例如服务器关闭或浏览器已离开页面
	 */
	public static final CloseStatus GOING_AWAY = new CloseStatus(1001);

	/**
	 * 1002表示由于协议错误, 端点正在终止连接
	 */
	public static final CloseStatus PROTOCOL_ERROR  = new CloseStatus(1002);

	/**
	 * 1003表示端点正在终止连接, 因为接收到它不能接受的数据类型 (e.g., 只能理解文本数据的端点如果收到二进制消息则可以发送它).
	 */
	public static final CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003);

	// 10004: Reserved.
	// 具体含义可能在将来定义.

	/**
	 * 1005是保留值, 不得在端点的关闭控制帧中设置为状态码.
	 * 用于期望状态码但实际上没有状态码的应用程序.
	 */
	public static final CloseStatus NO_STATUS_CODE = new CloseStatus(1005);

	/**
	 * 1006是保留值, 不得在端点的关闭控制帧中设置为状态码.
	 * 用于期望状态码指示连接异常关闭的应用程序, e.g., 不发送或接收关闭控制框架.
	 */
	public static final CloseStatus NO_CLOSE_FRAME = new CloseStatus(1006);

	/**
	 * 1007表示端点正在终止连接, 因为它已在消息中接收到与消息类型不一致的数据 (e.g., 文本消息中的非UTF-8 [RFC3629]数据).
	 */
	public static final CloseStatus BAD_DATA = new CloseStatus(1007);

	/**
	 * 1008表示端点正在终止连接, 因为它已收到违反其策略的消息.
	 * 这是一个通用状态代码, 当没有其他更合适的状态码 (e.g., 1003 或 1009) 或者需要隐藏有关策略的特定详细信息时, 可以返回该状态码.
	 */
	public static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008);

	/**
	 * 1009表示端点正在终止连接, 因为它收到的消息太大而无法处理
	 */
	public static final CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009);

	/**
	 * 1010表示端点 (客户端) 正在终止连接, 因为它期望服务器协商一个或多个扩展, 但服务器未在WebSocket握手的响应消息中返回它们.
	 * 需要的扩展列表应该出现在关闭帧的 /reason/ 部分中.
	 * 请注意, 服务器不使用此状态码, 因为它可能会使WebSocket握手失败.
	 */
	public static final CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010);

	/**
	 * 1011表示服务器正在终止连接, 因为它遇到意外情况, 无法满足请求
	 */
	public static final CloseStatus SERVER_ERROR = new CloseStatus(1011);

	/**
	 * 1012表示服务已重新启动.
	 * 客户端可能会重新连接, 如果选择这样做, 应使用5到30秒的随机延迟重新连接.
	 */
	public static final CloseStatus SERVICE_RESTARTED = new CloseStatus(1012);

	/**
	 * 1013表示该服务正在经历过载.
	 * 客户端应该只连接到不同的IP (当目标有多个IP时) 或者在用户操作时重新连接到同一个IP."
	 */
	public static final CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013);

	/**
	 * 1015是保留值, 不得在端点的关闭控制帧中设置为状态码.
	 * 用于期望状态代码指示由于未能执行TLS握手而关闭连接的应用程序 (e.g., 无法验证服务器证书).
	 */
	public static final CloseStatus TLS_HANDSHAKE_FAILURE = new CloseStatus(1015);

	/**
	 * 在框架内使用的状态码表示会话变得不可靠 (e.g. 在发送消息时超时), 并且应该特别小心,
	 * e.g. 避免在正常关机期间向客户端发送任何进一步的数据.
	 */
	public static final CloseStatus SESSION_NOT_RELIABLE = new CloseStatus(4500);


	private final int code;

	private final String reason;


	/**
	 * @param code 状态码
	 */
	public CloseStatus(int code) {
		this(code, null);
	}

	/**
	 * @param code 状态码
	 * @param reason 原因
	 */
	public CloseStatus(int code, String reason) {
		Assert.isTrue((code >= 1000 && code < 5000), "Invalid status code");
		this.code = code;
		this.reason = reason;
	}


	public int getCode() {
		return this.code;
	}

	public String getReason() {
		return this.reason;
	}

	/**
	 * 使用指定的原因从此创建一个新的{@link CloseStatus}.
	 * 
	 * @param reason 原因
	 * 
	 * @return 新的{@link CloseStatus}实例
	 */
	public CloseStatus withReason(String reason) {
		Assert.hasText(reason, "Reason must not be empty");
		return new CloseStatus(this.code, reason);
	}


	public boolean equalsCode(CloseStatus other) {
		return (this.code == other.code);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CloseStatus)) {
			return false;
		}
		CloseStatus otherStatus = (CloseStatus) other;
		return (this.code == otherStatus.code && ObjectUtils.nullSafeEquals(this.reason, otherStatus.reason));
	}

	@Override
	public int hashCode() {
		return this.code * 29 + ObjectUtils.nullSafeHashCode(this.reason);
	}

	@Override
	public String toString() {
		return "CloseStatus[code=" + this.code + ", reason=" + this.reason + "]";
	}

}
