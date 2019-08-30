package org.springframework.web.portlet.mvc;

import javax.portlet.EventRequest;
import javax.portlet.EventResponse;

/**
 * Portlet {@link Controller}接口的扩展, 允许处理Portlet 2.0事件请求.
 * 也可以由{@link AbstractController}子类实现.
 */
public interface EventAwareController {

	/**
	 * 处理事件请求.
	 * 
	 * @param request 当前的portlet事件请求
	 * @param response 当前的portlet事件响应
	 * 
	 * @throws Exception
	 */
	void handleEventRequest(EventRequest request, EventResponse response) throws Exception;

}
