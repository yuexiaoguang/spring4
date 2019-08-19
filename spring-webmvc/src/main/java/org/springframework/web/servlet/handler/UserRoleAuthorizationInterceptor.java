package org.springframework.web.servlet.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器, 由HttpServletRequest的isUserInRole方法评估, 通过用户的角色检查当前用户的授权.
 */
public class UserRoleAuthorizationInterceptor extends HandlerInterceptorAdapter {

	private String[] authorizedRoles;


	/**
	 * 设置此拦截器应视为已授权的角色.
	 * 
	 * @param authorizedRoles 角色名称数组
	 */
	public final void setAuthorizedRoles(String... authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}


	@Override
	public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		if (this.authorizedRoles != null) {
			for (String role : this.authorizedRoles) {
				if (request.isUserInRole(role)) {
					return true;
				}
			}
		}
		handleNotAuthorized(request, response, handler);
		return false;
	}

	/**
	 * 根据此拦截器处理未授权的请求.
	 * 默认实现发送HTTP状态码403 ("forbidden").
	 * <p>可以重写此方法以写入自定义消息, 转发或重定向到某个错误页面或登录页面, 或抛出ServletException.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @throws javax.servlet.ServletException 如果有内部错误
	 * @throws java.io.IOException 如果在写入响应时出现I/O错误
	 */
	protected void handleNotAuthorized(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
