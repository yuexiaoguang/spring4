package org.springframework.web.socket.adapter.standard;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.websocket.Extension;

import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.socket.WebSocketExtension;

/**
 * A sub-class of {@link org.springframework.web.socket.WebSocketExtension} that
 * can be constructed from an {@link javax.websocket.Extension}.
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
