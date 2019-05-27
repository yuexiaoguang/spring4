package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;

/**
 * A connected event represents the server response to a client's connect request.
 * See {@link org.springframework.web.socket.messaging.SessionConnectEvent}.
 */
@SuppressWarnings("serial")
public class SessionConnectedEvent extends AbstractSubProtocolEvent {

	/**
	 * Create a new SessionConnectedEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connected message
	 */
	public SessionConnectedEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectedEvent(Object source, Message<byte[]> message, Principal user) {
		super(source, message, user);
	}

}
