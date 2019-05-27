package org.springframework.web.socket.messaging;

import org.springframework.messaging.Message;

/**
 * A contract for handling sub-protocol errors sent to clients.
 */
public interface SubProtocolErrorHandler<P> {

	/**
	 * Handle errors thrown while processing client messages providing an
	 * opportunity to prepare the error message or to prevent one from being sent.
	 * <p>Note that the STOMP protocol requires a server to close the connection
	 * after sending an ERROR frame. To prevent an ERROR frame from being sent,
	 * a handler could return {@code null} and send a notification message
	 * through the broker instead, e.g. via a user destination.
	 * @param clientMessage the client message related to the error, possibly
	 * {@code null} if error occurred while parsing a WebSocket message
	 * @param ex the cause for the error, never {@code null}
	 * @return the error message to send to the client, or {@code null} in which
	 * case no message will be sent.
	 */
	Message<P> handleClientMessageProcessingError(Message<P> clientMessage, Throwable ex);

	/**
	 * Handle errors sent from the server side to clients, e.g. errors from the
	 * {@link org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler
	 * "broke relay"} because connectivity failed or the external broker sent an
	 * error message, etc.
	 * @param errorMessage the error message, never {@code null}
	 * @return the error message to send to the client, or {@code null} in which
	 * case no message will be sent.
	 */
	Message<P> handleErrorMessageToClient(Message<P> errorMessage);

}
