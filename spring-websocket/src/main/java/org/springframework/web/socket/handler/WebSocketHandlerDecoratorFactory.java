package org.springframework.web.socket.handler;

import org.springframework.web.socket.WebSocketHandler;

/**
 * A factory for applying decorators to a WebSocketHandler.
 *
 * <p>Decoration should be done through sub-classing
 * {@link org.springframework.web.socket.handler.WebSocketHandlerDecorator
 * WebSocketHandlerDecorator} to allow any code to traverse decorators and/or
 * unwrap the original handler when necessary .
 */
public interface WebSocketHandlerDecoratorFactory {

	/**
	 * Decorate the given WebSocketHandler.
	 * @param handler the handler to be decorated.
	 * @return the same handler or the handler wrapped with a sub-class of
	 * {@code WebSocketHandlerDecorator}.
	 */
	WebSocketHandler decorate(WebSocketHandler handler);

}
