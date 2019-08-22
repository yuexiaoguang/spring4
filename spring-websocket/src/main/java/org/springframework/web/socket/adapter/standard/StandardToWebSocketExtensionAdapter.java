package org.springframework.web.socket.adapter.standard;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.websocket.Extension;

import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.socket.WebSocketExtension;

/**
 * {@link org.springframework.web.socket.WebSocketExtension}的子类, 可以从{@link javax.websocket.Extension}构造.
 */
public class StandardToWebSocketExtensionAdapter extends WebSocketExtension {


	public StandardToWebSocketExtensionAdapter(Extension extension) {
		super(extension.getName(), initParameters(extension));
	}


	private static Map<String, String> initParameters(Extension extension) {
		List<Extension.Parameter> parameters = extension.getParameters();
		Map<String, String> result = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
		for (Extension.Parameter parameter : parameters) {
			result.put(parameter.getName(), parameter.getValue());
		}
		return result;
	}

}
