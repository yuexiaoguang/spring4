package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * 将请求参数从{@code ActionRequest}转发到{@code RenderRequest}的拦截器.
 *
 * <p>用于{@link ParameterHandlerMapping ParameterHandlerMapping}
 * 或{@link PortletModeParameterHandlerMapping PortletModeParameterHandlerMapping}.
 * 它将确保用于将{@code ActionRequest}映射到处理器的参数将转发到{@code RenderRequest}, 以便它也将以相同的方式映射.
 *
 * <p>使用此Interceptor时, 仍然可以更改处理器中映射参数的值, 以便更改渲染请求将映射的位置.
 *
 * <p>请注意, 此拦截器会调用{@code ActionResponse.setRenderParameter},
 * 这意味着将无法在处理器中调用{@code ActionResponse.sendRedirect}.
 * 如果需要重定向, 那么应该避免使用此拦截器, 并编写另一个以不同方式执行此操作的拦截器, 或者从处理器中手动转发参数.
 *
 * <p>Thanks to Rainer Schmitz for suggesting this mapping strategy!
 */
public class ParameterMappingInterceptor extends HandlerInterceptorAdapter {

	/** 用于映射到处理器的请求参数名称 */
	public final static String DEFAULT_PARAMETER_NAME = "action";

	private String parameterName = DEFAULT_PARAMETER_NAME;


	/**
	 * 设置用于映射的参数的名称.
	 */
	public void setParameterName(String parameterName) {
		this.parameterName = (parameterName != null ? parameterName : DEFAULT_PARAMETER_NAME);
	}


	/**
	 * 如果请求是{@link javax.portlet.ActionRequest ActionRequest}, 获取处理器映射参数并将其添加到ActionResponse.
	 */
	@Override
	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler) {
		String mappingParameter = request.getParameter(this.parameterName);
		if (mappingParameter != null) {
			response.setRenderParameter(parameterName, mappingParameter);
		}
		return true;
	}

}
