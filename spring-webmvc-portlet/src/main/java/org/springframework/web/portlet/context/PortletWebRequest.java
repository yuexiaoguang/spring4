package org.springframework.web.portlet.context;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link javax.portlet.PortletRequest}的
 * {@link org.springframework.web.context.request.WebRequest}适配器.
 */
public class PortletWebRequest extends PortletRequestAttributes implements NativeWebRequest {

	/**
	 * @param request 当前的portlet请求
	 */
	public PortletWebRequest(PortletRequest request) {
		super(request);
	}

	/**
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 */
	public PortletWebRequest(PortletRequest request, PortletResponse response) {
		super(request, response);
	}


	@Override
	public Object getNativeRequest() {
		return getRequest();
	}

	@Override
	public Object getNativeResponse() {
		return getResponse();
	}

	@Override
	public <T> T getNativeRequest(Class<T> requiredType) {
		return PortletUtils.getNativeRequest(getRequest(), requiredType);
	}

	@Override
	public <T> T getNativeResponse(Class<T> requiredType) {
		return PortletUtils.getNativeResponse(getResponse(), requiredType);
	}


	@Override
	public String getHeader(String headerName) {
		return getRequest().getProperty(headerName);
	}

	@Override
	public String[] getHeaderValues(String headerName) {
		String[] headerValues = StringUtils.toStringArray(getRequest().getProperties(headerName));
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getPropertyNames());
	}

	@Override
	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	@Override
	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	@Override
	public Iterator<String> getParameterNames() {
		return CollectionUtils.toIterator(getRequest().getParameterNames());
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return getRequest().getParameterMap();
	}

	@Override
	public Locale getLocale() {
		return getRequest().getLocale();
	}

	@Override
	public String getContextPath() {
		return getRequest().getContextPath();
	}

	@Override
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	@Override
	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return getRequest().isSecure();
	}

	/**
	 * portlet请求不支持 Last-modified的处理: 因此, 此方法始终返回{@code false}.
	 */
	@Override
	public boolean checkNotModified(long lastModifiedTimestamp) {
		return false;
	}

	/**
	 * portlet请求不支持 Last-modified的处理: 因此, 此方法始终返回{@code false}.
	 */
	@Override
	public boolean checkNotModified(String eTag) {
		return false;
	}

	/**
	 * portlet请求不支持 Last-modified的处理: 因此, 此方法始终返回{@code false}.
	 */
	@Override
	public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
		return false;
	}

	@Override
	public String getDescription(boolean includeClientInfo) {
		PortletRequest request = getRequest();
		StringBuilder result = new StringBuilder();
		result.append("context=").append(request.getContextPath());
		if (includeClientInfo) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				result.append(";session=").append(session.getId());
			}
			String user = getRequest().getRemoteUser();
			if (StringUtils.hasLength(user)) {
				result.append(";user=").append(user);
			}
		}
		return result.toString();
	}


	@Override
	public String toString() {
		return "PortletWebRequest: " + getDescription(true);
	}

}
