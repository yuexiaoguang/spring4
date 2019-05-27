package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;

/**
 * Event raised when a new WebSocket client using a Simple Messaging Protocol
 * (e.g. STOMP) sends a subscription request.
 */
@SuppressWarnings("serial")
public class SessionSubscribeEvent extends AbstractSubProtocolEvent {

	public SessionSubscribeEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionSubscribeEvent(Object source, Message<byte[]> message, Principal user) {
		super(source, message, user);
	}

}
