package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;

/**
 * {@link org.springframework.web.socket.sockjs.SockJsService}的默认实现,
 * 所有默认的{@link TransportHandler}实现都已预先注册.
 */
public class DefaultSockJsService extends TransportHandlingSockJsService implements ServletContextAware {

	/**
	 * @param scheduler 用于心跳消息和删除超时会话的任务调度器;
	 * 提供的TaskScheduler应声明为Spring bean, 以确保它在启动时初始化并在应用程序停止时关闭.
	 */
	public DefaultSockJsService(TaskScheduler scheduler) {
		this(scheduler, getDefaultTransportHandlers(null));
	}

	/**
	 * 使用重写的{@link TransportHandler handler}类型替换相应的默认处理器实现.
	 * 
	 * @param scheduler 用于心跳消息和删除超时会话的任务调度器;
	 * 提供的TaskScheduler应声明为Spring bean, 以确保它在启动时初始化并在应用程序停止时关闭.
	 * @param handlerOverrides 零或多个传输处理器
	 */
	public DefaultSockJsService(TaskScheduler scheduler, TransportHandler... handlerOverrides) {
		this(scheduler, Arrays.asList(handlerOverrides));
	}

	/**
	 * 使用重写的{@link TransportHandler handler}类型替换相应的默认处理器实现.
	 * 
	 * @param scheduler 用于心跳消息和删除超时会话的任务调度器;
	 * 提供的TaskScheduler应声明为Spring bean, 以确保它在启动时初始化并在应用程序停止时关闭.
	 * @param handlerOverrides 零或多个传输处理器
	 */
	public DefaultSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlerOverrides) {
		super(scheduler, getDefaultTransportHandlers(handlerOverrides));
	}


	@SuppressWarnings("deprecation")
	private static Set<TransportHandler> getDefaultTransportHandlers(Collection<TransportHandler> overrides) {
		Set<TransportHandler> result = new LinkedHashSet<TransportHandler>(8);
		result.add(new XhrPollingTransportHandler());
		result.add(new XhrReceivingTransportHandler());
		result.add(new XhrStreamingTransportHandler());
		result.add(new JsonpPollingTransportHandler());
		result.add(new JsonpReceivingTransportHandler());
		result.add(new EventSourceTransportHandler());
		result.add(new HtmlFileTransportHandler());
		try {
			result.add(new WebSocketTransportHandler(new DefaultHandshakeHandler()));
		}
		catch (Exception ex) {
			Log logger = LogFactory.getLog(DefaultSockJsService.class);
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to create a default WebSocketTransportHandler", ex);
			}
		}
		if (overrides != null) {
			result.addAll(overrides);
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		for (TransportHandler handler : getTransportHandlers().values()) {
			if (handler instanceof ServletContextAware) {
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}
}
