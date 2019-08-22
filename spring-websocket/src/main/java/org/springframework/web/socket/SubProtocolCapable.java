package org.springframework.web.socket;

import java.util.List;

/**
 * WebSocket处理器的接口, 支持RFC 6455中定义的子协议.
 */
public interface SubProtocolCapable {

	/**
	 * 返回支持的子协议列表.
	 */
	List<String> getSubProtocols();

}
