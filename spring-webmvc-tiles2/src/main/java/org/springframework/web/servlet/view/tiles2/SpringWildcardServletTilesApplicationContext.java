package org.springframework.web.servlet.view.tiles2;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.ServletContext;

import org.apache.tiles.servlet.context.ServletTilesApplicationContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * Spring-specific subclass of the Tiles ServletTilesApplicationContext.
 *
 * <p><b>NOTE: Tiles 2 support is deprecated in favor of Tiles 3 and will be removed
 * as of Spring Framework 5.0.</b>.
 *
 * @deprecated as of Spring 4.2, in favor of Tiles 3
 */
@Deprecated
public class SpringWildcardServletTilesApplicationContext extends ServletTilesApplicationContext {

	private final ResourcePatternResolver resolver;


	public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
		super(servletContext);
		this.resolver = new ServletContextResourcePatternResolver(servletContext);
	}


	@Override
	public URL getResource(String path) throws IOException {
		Set<URL> urlSet = getResources(path);
		if (!CollectionUtils.isEmpty(urlSet)) {
			return urlSet.iterator().next();
		}
		return null;
	}

	@Override
	public Set<URL> getResources(String path) throws IOException {
		Set<URL> urlSet = null;
		Resource[] resources = this.resolver.getResources(path);
		if (!ObjectUtils.isEmpty(resources)) {
			urlSet = new LinkedHashSet<URL>(resources.length);
			for (Resource resource : resources) {
				urlSet.add(resource.getURL());
			}
		}
		return urlSet;
	}

}
