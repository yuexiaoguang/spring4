package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.cors.CorsConfiguration;

/**
 * {@code CorsRegistry}协助注册映射到路径模式的{@link CorsConfiguration}.
 */
public class CorsRegistry {

	private final List<CorsRegistration> registrations = new ArrayList<CorsRegistration>();


	/**
	 * 为指定的路径模式启用跨源请求处理.
	 * <p>支持精确路径映射URI (例如{@code "/admin"}), 以及Ant样式路径模式 (例如{@code "/admin/**"}).
	 * <p>默认情况下, 允许所有来源, 所有header, 凭据和{@code GET}, {@code HEAD}, and {@code POST}方法,
	 * 最长期限设置为30分钟.
	 * 
	 * @param pathPattern 用于启用CORS处理的路径模式
	 * 
	 * @return 对应注册对象的CorsRegistration, 允许进一步微调
	 */
	public CorsRegistration addMapping(String pathPattern) {
		CorsRegistration registration = new CorsRegistration(pathPattern);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 返回已注册的{@link CorsConfiguration}对象, 由路径模式键入.
	 */
	protected Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> configs = new LinkedHashMap<String, CorsConfiguration>(this.registrations.size());
		for (CorsRegistration registration : this.registrations) {
			configs.put(registration.getPathPattern(), registration.getCorsConfiguration());
		}
		return configs;
	}

}
