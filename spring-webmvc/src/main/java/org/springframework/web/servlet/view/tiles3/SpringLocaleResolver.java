package org.springframework.web.servlet.view.tiles3;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.NotAServletEnvironmentException;
import org.apache.tiles.request.servlet.ServletUtil;

import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tiles LocaleResolver adapter that delegates to a Spring
 * {@link org.springframework.web.servlet.LocaleResolver}, exposing the
 * DispatcherServlet-managed locale.
 *
 * <p>This adapter gets automatically registered by {@link TilesConfigurer}.
 */
public class SpringLocaleResolver extends DefaultLocaleResolver {

	@Override
	public Locale resolveLocale(Request request) {
		try {
			HttpServletRequest servletRequest = ServletUtil.getServletRequest(request).getRequest();
			if (servletRequest != null) {
				return RequestContextUtils.getLocale(servletRequest);
			}
		}
		catch (NotAServletEnvironmentException ex) {
			// ignore
		}
		return super.resolveLocale(request);
	}

}
