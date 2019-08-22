package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * {@link WebSocketConfigurationSupport}的变体,
 * 用于检测Spring配置中{@link WebSocketConfigurer}的实现, 并调用它们以配置WebSocket请求处理.
 */
@Configuration
public class DelegatingWebSocketConfiguration extends WebSocketConfigurationSupport {

	private final List<WebSocketConfigurer> configurers = new ArrayList<WebSocketConfigurer>();


	@Autowired(required = false)
	public void setConfigurers(List<WebSocketConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}


	@Override
	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		for (WebSocketConfigurer configurer : this.configurers) {
			configurer.registerWebSocketHandlers(registry);
		}
	}

}
