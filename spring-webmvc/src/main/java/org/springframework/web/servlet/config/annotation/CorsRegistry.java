package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.cors.CorsConfiguration;

/**
 * {@code CorsRegistry} assists with the registration of {@link CorsConfiguration}
 * mapped to a path pattern.
 */
public class CorsRegistry {

	private final List<CorsRegistration> registrations = new ArrayList<CorsRegistration>();


	/**
	 * Enable cross-origin request handling for the specified path pattern.
	 * <p>Exact path mapping URIs (such as {@code "/admin"}) are supported as
	 * well as Ant-style path patterns (such as {@code "/admin/**"}).
	 * <p>By default, all origins, all headers, credentials and {@code GET},
	 * {@code HEAD}, and {@code POST} methods are allowed, and the max age
	 * is set to 30 minutes.
	 * @param pathPattern the path pattern to enable CORS handling for
	 * @return CorsRegistration the corresponding registration object,
	 * allowing for further fine-tuning
	 */
	public CorsRegistration addMapping(String pathPattern) {
		CorsRegistration registration = new CorsRegistration(pathPattern);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Return the registered {@link CorsConfiguration} objects,
	 * keyed by path pattern.
	 */
	protected Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> configs = new LinkedHashMap<String, CorsConfiguration>(this.registrations.size());
		for (CorsRegistration registration : this.registrations) {
			configs.put(registration.getPathPattern(), registration.getCorsConfiguration());
		}
		return configs;
	}

}
