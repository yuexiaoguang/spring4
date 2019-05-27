package org.springframework.jms.config;

import javax.jms.MessageListener;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * {@link JmsListenerEndpoint}, 只是提供要调用的{@link MessageListener}来处理这个端点的传入消息.
 */
public class SimpleJmsListenerEndpoint extends AbstractJmsListenerEndpoint {

	private MessageListener messageListener;


	/**
	 * 设置在收到与端点匹配的消息时调用的{@link MessageListener}.
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * 返回在收到与端点匹配的消息时调用的{@link MessageListener}.
	 */
	public MessageListener getMessageListener() {
		return this.messageListener;
	}


	@Override
	protected MessageListener createMessageListener(MessageListenerContainer container) {
		return getMessageListener();
	}

	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | messageListener='").append(this.messageListener).append("'");
	}

}
