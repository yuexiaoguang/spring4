package org.springframework.messaging.simp;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.util.Assert;

/**
 * 检查消息类型的消息条件.
 */
public class SimpMessageTypeMessageCondition extends AbstractMessageCondition<SimpMessageTypeMessageCondition> {

	public static final SimpMessageTypeMessageCondition MESSAGE =
			new SimpMessageTypeMessageCondition(SimpMessageType.MESSAGE);

	public static final SimpMessageTypeMessageCondition SUBSCRIBE =
			new SimpMessageTypeMessageCondition(SimpMessageType.SUBSCRIBE);


	private final SimpMessageType messageType;


	/**
	 * @param messageType 要将消息匹配的消息类型
	 */
	public SimpMessageTypeMessageCondition(SimpMessageType messageType) {
		Assert.notNull(messageType, "MessageType must not be null");
		this.messageType = messageType;
	}


	public SimpMessageType getMessageType() {
		return this.messageType;
	}

	@Override
	protected Collection<?> getContent() {
		return Arrays.asList(this.messageType);
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	@Override
	public SimpMessageTypeMessageCondition combine(SimpMessageTypeMessageCondition other) {
		return other;
	}

	@Override
	public SimpMessageTypeMessageCondition getMatchingCondition(Message<?> message) {
		Object actualMessageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		if (actualMessageType == null) {
			return null;
		}
		return this;
	}

	@Override
	public int compareTo(SimpMessageTypeMessageCondition other, Message<?> message) {
		Object actualMessageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		if (actualMessageType != null) {
			if (actualMessageType.equals(this.getMessageType()) && actualMessageType.equals(other.getMessageType())) {
				return 0;
			}
			else if (actualMessageType.equals(this.getMessageType())) {
				return -1;
			}
			else if (actualMessageType.equals(other.getMessageType())) {
				return 1;
			}
		}
		return 0;
	}

}
