package org.springframework.web.socket.sockjs.frame;

/**
 * 将特定于传输的格式应用于SockJS帧的内容, 从而生成可以写出的内容.
 * 主要用于推送数据的HTTP服务器端传输.
 *
 * <p>格式化可能不同于简单地为XHR轮询和流传输添加新行字符, jsonp样式回调函数, 周围脚本标记等等.
 *
 * <p>对于正在使用的各种SockJS帧格式, 请参阅
 * {@link  org.springframework.web.socket.sockjs.transport.handler
 * .AbstractHttpSendingTransportHandler#getFrameFormat(org.springframework.http.server.ServerHttpRequest)
 * AbstractHttpSendingTransportHandler.getFrameFormat}的实现
 */
public interface SockJsFrameFormat {

	String format(SockJsFrame frame);

}
