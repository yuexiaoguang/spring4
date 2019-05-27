package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;

/**
 * Event raised when the session of a WebSocket client using a Simple Messaging
 * Protocol (e.g. STOMP) as the WebSocket sub-protocol is closed.
 *
 * <p>Note that this event may be raised more than once for a single session and
 * therefore event consumers should be idempotent and ignore a duplicate event.
 */
@SuppressWarnings("serial")
public class SessionDisconnectEvent extends AbstractSubProtocolEvent {

	private final String sessionId;

	private final CloseStatus status;


	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus) {

		this(source, message, sessionId, closeStatus, null);
	}

	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 * @param user the current session user
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus, Principal user) {

		super(source, message, user);
		Assert.notNull(sessionId, "Session id must not be null");
		this.sessionId = sessionId;
		this.status = closeStatus;
	}


	/**
	 * Return the session id.
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Return the status with which the session was closed.
	 */
	public CloseStatus getCloseStatus() {
		return this.status;
	}


	@Override
	public String toString() {
		return "SessionDisconnectEvent[sessionId=" + this.sessionId + ", " +
				(this.status != null ? this.status.toString() : "closeStatus=null") + "]";
	}

}
