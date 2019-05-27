package org.springframework.web.servlet.view.tiles2;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.jsp.context.JspTilesRequestContext;
import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.servlet.context.ServletTilesRequestContext;

import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tiles LocaleResolver adapter that delegates to a Spring
 * {@link org.springframework.web.servlet.LocaleResolver},
 * exposing the DispatcherServlet-managed locale.
 *
 * <p>This adapter gets automatically registered by {@link TilesConfigurer}.
 * If you are using standard Tiles bootstrap, specify the name of this class
 * as value for the init-param "org.apache.tiles.locale.LocaleResolver".
 *
 * <p><b>NOTE: Tiles 2 support is deprecated in favor of Tiles 3 and will be removed
 * as of Spring Framework 5.0.</b>.
 *
 * @deprecated as of Spring 4.2, in favor of Tiles 3
 */
@Deprecated
public class SpringLocaleResolver extends DefaultLocaleResolver {

	@Override
	public Locale resolveLocale(TilesRequestContext context) {
		if (context instanceof JspTilesRequestContext) {
			PageContext pc = ((JspTilesRequestContext) context).getPageContext();
			return RequestContextUtils.getLocale((HttpServletRequest) pc.getRequest());
		}
		else if (context instanceof ServletTilesRequestContext) {
			HttpServletRequest request = ((ServletTilesRequestContext) context).getRequest();
			if (request != null) {
				return RequestContextUtils.getLocale(request);
			}
		}
		return super.resolveLocale(context);
	}

}
