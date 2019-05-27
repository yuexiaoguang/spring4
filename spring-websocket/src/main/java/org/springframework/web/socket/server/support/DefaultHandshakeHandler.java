package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A default {@link org.springframework.web.socket.server.HandshakeHandler} implementation,
 * extending {@link AbstractHandshakeHandler} with Servlet-specific initialization support.
 * See {@link AbstractHandshakeHandler}'s javadoc for details on supported servers etc.
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
