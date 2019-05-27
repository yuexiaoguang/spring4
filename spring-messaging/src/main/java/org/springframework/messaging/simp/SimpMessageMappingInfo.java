package org.springframework.messaging.simp;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.MessageCondition;

/**
 * 封装以下请求映射条件:
 * <ol>
 * <li>{@link SimpMessageTypeMessageCondition}
 * <li>{@link DestinationPatternsMessageCondition}
 * </ol>
 */
public class SimpMessageMappingInfo implements MessageCondition<SimpMessageMappingInfo> {

	private final SimpMessageTypeMessageCondition messageTypeMessageCondition;

	private final DestinationPatternsMessageCondition destinationConditions;


	public SimpMessageMappingInfo(SimpMessageTypeMessageCondition messageTypeMessageCondition,
			DestinationPatternsMessageCondition destinationConditions) {

		this.messageTypeMessageCondition = messageTypeMessageCondition;
		this.destinationConditions = destinationConditions;
	}


	public SimpMessageTypeMessageCondition getMessageTypeMessageCondition() {
		return this.messageTypeMessageCondition;
	}

	public DestinationPatternsMessageCondition getDestinationConditions() {
		return this.destinationConditions;
	}


	@Override
	public SimpMessageMappingInfo combine(SimpMessageMappingInfo other) {
		SimpMessageTypeMessageCondition typeCond =
				this.getMessageTypeMessageCondition().combine(other.getMessageTypeMessageCondition());
		DestinationPatternsMessageCondition destCond =
				this.destinationConditions.combine(other.getDestinationConditions());
		return new SimpMessageMappingInfo(typeCond, destCond);
	}

	@Override
	public SimpMessageMappingInfo getMatchingCondition(Message<?> message) {
		SimpMessageTypeMessageCondition typeCond = this.messageTypeMessageCondition.getMatchingCondition(message);
		if (typeCond == null) {
			return null;
		}
		DestinationPatternsMessageCondition destCond = this.destinationConditions.getMatchingCondition(message);
		if (destCond == null) {
			return null;
		}
		return new SimpMessageMappingInfo(typeCond, destCond);
	}

	@Override
	public int compareTo(SimpMessageMappingInfo other, Message<?> message) {
		int result = this.messageTypeMessageCondition.compareTo(other.messageTypeMessageCondition, message);
		if (result != 0) {
			return result;
		}
		result = this.destinationConditions.compareTo(other.destinationConditions, message);
		if (result != 0) {
			return result;
		}
		return 0;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof SimpMessageMappingInfo) {
			SimpMessageMappingInfo other = (SimpMessageMappingInfo) obj;
			return (this.destinationConditions.equals(other.destinationConditions) &&
					this.messageTypeMessageCondition.equals(other.messageTypeMessageCondition));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.destinationConditions.hashCode() * 31 + this.messageTypeMessageCondition.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(this.destinationConditions);
		builder.append(",messageType=").append(this.messageTypeMessageCondition);
		builder.append('}');
		return builder.toString();
	}

}
