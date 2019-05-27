package org.springframework.messaging.simp.broker;

import org.springframework.context.ApplicationEvent;

/**
 * 当代理的可用性发生变化时的事件
 */
public class BrokerAvailabilityEvent extends ApplicationEvent {

	private static final long serialVersionUID = -8156742505179181002L;

	private final boolean brokerAvailable;


	/**
	 * @param brokerAvailable {@code true} 如果代理可用, 否则{@code false}
	 * @param source 作为代理的组件, 或作为已更改可用性的外部代理的中继的组件. 不能是{@code null}.
	 */
	public BrokerAvailabilityEvent(boolean brokerAvailable, Object source) {
		super(source);
		this.brokerAvailable = brokerAvailable;
	}

	public boolean isBrokerAvailable() {
		return this.brokerAvailable;
	}

	@Override
	public String toString() {
		return "BrokerAvailabilityEvent[available=" + this.brokerAvailable + ", " + getSource() + "]";
	}

}
