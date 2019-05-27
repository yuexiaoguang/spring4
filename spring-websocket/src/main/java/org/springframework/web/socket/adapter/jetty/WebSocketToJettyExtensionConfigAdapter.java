package org.springframework.web.socket.adapter.jetty;

import java.util.Map;

import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;

import org.springframework.web.socket.WebSocketExtension;

public class WebSocketToJettyExtensionConfigAdapter extends ExtensionConfig {

	public WebSocketToJettyExtensionConfigAdapter(WebSocketExtension extension) {
		super(extension.getName());
		for (Map.Entry<String,String> parameter : extension.getParameters().entrySet()) {
			super.setParameter(parameter.getKey(), parameter.getValue());
		}
	}

}
