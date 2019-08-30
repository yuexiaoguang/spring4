package org.springframework.web.portlet.mvc;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * <p>将PortletMode转换为视图名称的简单控制器.
 * 这里的优点是客户端不会暴露于具体的视图技术, 而只是暴露给控制器URL; 具体视图将由ViewResolver确定.</p>
 *
 * <p>Example: PortletMode.VIEW -> "view"</p>
 *
 * <p>此控制器不处理操作请求.</p>
 */
public class PortletModeNameViewController implements Controller {

	@Override
	public void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception {
		throw new PortletException("PortletModeNameViewController does not handle action requests");
	}

	@Override
	public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) {
		return new ModelAndView(request.getPortletMode().toString());
	}

}
