package org.springframework.web.portlet.handler;

import java.io.IOException;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSecurityException;

/**
 * Interceptor that checks the authorization of the current user via the
 * user's roles, as evaluated by PortletRequest's isUserInRole method.
 */
public class UserRoleAuthorizationInterceptor extends HandlerInterceptorAdapter {

	private String[] authorizedRoles;


	/**
	 * Set the roles that this interceptor should treat as authorized.
	 * @param authorizedRoles array of role names
	 */
	public final void setAuthorizedRoles(String... authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}


	@Override
	public final boolean preHandle(PortletRequest request, PortletResponse response, Object handler)
			throws PortletException, IOException {

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
	 * Handle a request that is not authorized according to this interceptor.
	 * Default implementation throws a new PortletSecurityException.
	 * <p>This method can be overridden to write a custom message, forward or
	 * redirect to some error page or login page, or throw a PortletException.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @throws javax.portlet.PortletException if there is an internal error
	 * @throws java.io.IOException in case of an I/O error when writing the response
	 */
	protected void handleNotAuthorized(PortletRequest request, PortletResponse response, Object handler)
			throws PortletException, IOException {

		throw new PortletSecurityException("Request not authorized");
	}

}
