package org.springframework.web.socket.sockjs;

/**
 * Indicates a serious failure that occurred in the SockJS implementation as opposed to
 * in user code (e.g. IOException while writing to the response). When this exception
 * is raised, the SockJS session is typically closed.
 */
@SuppressWarnings("serial")
public class SockJsTransportFailureException extends SockJsException {

	/**
	 * Constructor for SockJsTransportFailureException.
	 * @param message the exception message
	 * @param cause the root cause
	 * @since 4.1.7
	 */
	public SockJsTransportFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor for SockJsTransportFailureException.
	 * @param message the exception message
	 * @param sessionId the SockJS session id
	 * @param cause the root cause
	 */
	public SockJsTransportFailureException(String message, String sessionId, Throwable cause) {
		super(message, sessionId, cause);
	}

}
