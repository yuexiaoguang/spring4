package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.simp.SimpMessageType;

/**
 * 表示STOMP命令.
 */
public enum StompCommand {

	// client
	STOMP(SimpMessageType.CONNECT),
	CONNECT(SimpMessageType.CONNECT),
	DISCONNECT(SimpMessageType.DISCONNECT),
	SUBSCRIBE(SimpMessageType.SUBSCRIBE, true, true, false),
	UNSUBSCRIBE(SimpMessageType.UNSUBSCRIBE, false, true, false),
	SEND(SimpMessageType.MESSAGE, true, false, true),
	ACK(SimpMessageType.OTHER),
	NACK(SimpMessageType.OTHER),
	BEGIN(SimpMessageType.OTHER),
	COMMIT(SimpMessageType.OTHER),
	ABORT(SimpMessageType.OTHER),

	// server
	CONNECTED(SimpMessageType.OTHER),
	RECEIPT(SimpMessageType.OTHER),
	MESSAGE(SimpMessageType.MESSAGE, true, true, true),
	ERROR(SimpMessageType.OTHER, false, false, true);


	private final SimpMessageType messageType;

	private final boolean destination;

	private final boolean subscriptionId;

	private final boolean body;


	StompCommand(SimpMessageType messageType) {
		this(messageType, false, false, false);
	}

	StompCommand(SimpMessageType messageType, boolean destination, boolean subscriptionId, boolean body) {
		this.messageType = messageType;
		this.destination = destination;
		this.subscriptionId = subscriptionId;
		this.body = body;
	}


	public SimpMessageType getMessageType() {
		return this.messageType;
	}

	public boolean requiresDestination() {
		return this.destination;
	}

	public boolean requiresSubscriptionId() {
		return this.subscriptionId;
	}

	public boolean requiresContentLength() {
		return this.body;
	}

	public boolean isBodyAllowed() {
		return this.body;
	}

}
