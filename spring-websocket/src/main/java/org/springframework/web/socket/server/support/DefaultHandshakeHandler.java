package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * 一个默认的{@link org.springframework.web.socket.server.HandshakeHandler}实现,
 * 使用Servlet特定的初始化支持扩展{@link AbstractHandshakeHandler}.
 * 有关支持的服务器等的详细信息, 请参阅{@link AbstractHandshakeHandler}的javadoc.
 */
public class DefaultHandshakeHandler extends AbstractHandshakeHandler implements ServletContextAware {

	public DefaultHandshakeHandler() {
	}

	public DefaultHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		super(requestUpgradeStrategy);
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		RequestUpgradeStrategy strategy = getRequestUpgradeStrategy();
		if (strategy instanceof ServletContextAware) {
			((ServletContextAware) strategy).setServletContext(servletContext);
		}
	}

}
