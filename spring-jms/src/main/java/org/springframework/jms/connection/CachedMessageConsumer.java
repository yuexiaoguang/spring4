package org.springframework.jms.connection;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * JMS MessageConsumer装饰器, 它适配所有对下面共享的MessageConsumer实例的调用.
 */
class CachedMessageConsumer implements MessageConsumer, QueueReceiver, TopicSubscriber {

	protected final MessageConsumer target;


	public CachedMessageConsumer(MessageConsumer target) {
		this.target = target;
	}


	@Override
	public String getMessageSelector() throws JMSException {
		return this.target.getMessageSelector();
	}

	@Override
	public Queue getQueue() throws JMSException {
		return (this.target instanceof QueueReceiver ? ((QueueReceiver) this.target).getQueue() : null);
	}

	@Override
	public Topic getTopic() throws JMSException {
		return (this.target instanceof TopicSubscriber ? ((TopicSubscriber) this.target).getTopic() : null);
	}

	@Override
	public boolean getNoLocal() throws JMSException {
		return (this.target instanceof TopicSubscriber && ((TopicSubscriber) this.target).getNoLocal());
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return this.target.getMessageListener();
	}

	@Override
	public void setMessageListener(MessageListener messageListener) throws JMSException {
		this.target.setMessageListener(messageListener);
	}

	@Override
	public Message receive() throws JMSException {
		return this.target.receive();
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		return this.target.receive(timeout);
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		return this.target.receiveNoWait();
	}

	@Override
	public void close() throws JMSException {
		// It's a cached MessageConsumer...
	}


	@Override
	public String toString() {
		return "Cached JMS MessageConsumer: " + this.target;
	}

}
