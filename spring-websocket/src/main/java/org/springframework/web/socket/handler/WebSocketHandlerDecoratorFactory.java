package org.springframework.web.socket.handler;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于将装饰器应用于WebSocketHandler的工厂.
 *
 * <p>装饰应该通过对
 * {@link org.springframework.web.socket.handler.WebSocketHandlerDecorator WebSocketHandlerDecorator}
 * 进行子类化来完成，以允许任何代码遍历装饰器和/或解包原始处理器.
 */
public interface WebSocketHandlerDecoratorFactory {

	/**
	 * 装饰给定的WebSocketHandler.
	 * 
	 * @param handler 要装饰的处理器
	 * 
	 * @return 相同的处理器, 或使用{@code WebSocketHandlerDecorator}的子类包装的处理器.
	 */
	WebSocketHandler decorate(WebSocketHandler handler);

}
