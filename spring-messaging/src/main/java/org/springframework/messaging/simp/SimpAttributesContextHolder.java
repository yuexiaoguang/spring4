package org.springframework.messaging.simp;

import org.springframework.core.NamedThreadLocal;
import org.springframework.messaging.Message;

/**
 * 以线程绑定的{@link SimpAttributes}对象的形式公开与会话关联的SiMP属性 (e.g. WebSocket).
 */
public abstract class SimpAttributesContextHolder {

	private static final ThreadLocal<SimpAttributes> attributesHolder =
			new NamedThreadLocal<SimpAttributes>("SiMP session attributes");


	/**
	 * 重置当前线程的SimpAttributes.
	 */
	public static void resetAttributes() {
		attributesHolder.remove();
	}

	/**
	 * 将给定的SimpAttributes绑定到当前线程
	 * 
	 * @param attributes 要公开的RequestAttributes
	 */
	public static void setAttributes(SimpAttributes attributes) {
		if (attributes != null) {
			attributesHolder.set(attributes);
		}
		else {
			resetAttributes();
		}
	}

	/**
	 * 从给定消息中提取SiMP会话属性, 将它们包装在{@link SimpAttributes}实例中并将其绑定到当前线程
	 * 
	 * @param message 从中提取会话属性的消息
	 */
	public static void setAttributesFromMessage(Message<?> message) {
		setAttributes(SimpAttributes.fromMessage(message));
	}

	/**
	 * 返回当前绑定到该线程的SimpAttributes.
	 * 
	 * @return 属性或{@code null}
	 */
	public static SimpAttributes getAttributes() {
		return attributesHolder.get();
	}

	/**
	 * 返回当前绑定到该线程的SimpAttributes, 或如果没有绑定则引发{@link java.lang.IllegalStateException}.
	 * 
	 * @return 属性, never {@code null}
	 * @throws java.lang.IllegalStateException 如果属性没有绑定
	 */
	public static SimpAttributes currentAttributes() throws IllegalStateException {
		SimpAttributes attributes = getAttributes();
		if (attributes == null) {
			throw new IllegalStateException("No thread-bound SimpAttributes found. " +
					"Your code is probably not processing a client message and executing in " +
					"message-handling methods invoked by the SimpAnnotationMethodMessageHandler?");
		}
		return attributes;
	}

}
