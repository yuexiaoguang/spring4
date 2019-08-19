package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 工具类, 用于从{@code HandlerMapping}获取可用于特定请求的信息.
 *
 * <p>提供以下方法:
 * <ul>
 * <li>{@link #getMatchableHandlerMapping} &mdash; 获取{@code HandlerMapping}以检查请求匹配条件.
 * <li>{@link #getCorsConfiguration} &mdash; 获取请求的CORS配置.
 * </ul>
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private List<HandlerMapping> handlerMappings;


	public HandlerMappingIntrospector() {
	}

	/**
	 * 构造函数, 用于检测给定{@code ApplicationContext}中已配置的{@code HandlerMapping},
	 * 或者回退到"DispatcherServlet.properties", 如{@code DispatcherServlet}.
	 * 
	 * @deprecated as of 4.3.12, in favor of {@link #setApplicationContext}
	 */
	@Deprecated
	public HandlerMappingIntrospector(ApplicationContext context) {
		this.handlerMappings = initHandlerMappings(context);
	}


	/**
	 * 返回配置的HandlerMapping.
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? this.handlerMappings : Collections.<HandlerMapping>emptyList());
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			this.handlerMappings = initHandlerMappings(this.applicationContext);
		}
	}


	/**
	 * 查找将处理给定请求的{@link HandlerMapping}, 并将其作为{@link MatchableHandlerMapping}返回, 可用于测试请求匹配条件.
	 * <p>如果匹配的HandlerMapping不是{@link MatchableHandlerMapping}的实例, 则会引发 IllegalStateException.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 解析的匹配器, 或{@code null}
	 * @throws Exception 如果任何HandlerMapping引发异常
	 */
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		Assert.notNull(this.handlerMappings, "Handler mappings not initialized");
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			Object handler = handlerMapping.getHandler(wrapper);
			if (handler == null) {
				continue;
			}
			if (handlerMapping instanceof MatchableHandlerMapping) {
				return ((MatchableHandlerMapping) handlerMapping);
			}
			throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
		}
		return null;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		Assert.notNull(this.handlerMappings, "Handler mappings not initialized");
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			HandlerExecutionChain handler = null;
			try {
				handler = handlerMapping.getHandler(wrapper);
			}
			catch (Exception ex) {
				// Ignore
			}
			if (handler == null) {
				continue;
			}
			if (handler.getInterceptors() != null) {
				for (HandlerInterceptor interceptor : handler.getInterceptors()) {
					if (interceptor instanceof CorsConfigurationSource) {
						return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrapper);
					}
				}
			}
			if (handler.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) handler.getHandler()).getCorsConfiguration(wrapper);
			}
		}
		return null;
	}


	private static List<HandlerMapping> initHandlerMappings(ApplicationContext applicationContext) {
		Map<String, HandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				applicationContext, HandlerMapping.class, true, false);
		if (!beans.isEmpty()) {
			List<HandlerMapping> mappings = new ArrayList<HandlerMapping>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return Collections.unmodifiableList(mappings);
		}
		return Collections.unmodifiableList(initFallback(applicationContext));
	}

	private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
		Properties props;
		String path = "DispatcherServlet.properties";
		try {
			Resource resource = new ClassPathResource(path, DispatcherServlet.class);
			props = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + path + "': " + ex.getMessage());
		}

		String value = props.getProperty(HandlerMapping.class.getName());
		String[] names = StringUtils.commaDelimitedListToStringArray(value);
		List<HandlerMapping> result = new ArrayList<HandlerMapping>(names.length);
		for (String name : names) {
			try {
				Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
				Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}


	/**
	 * 忽略请求属性更改的请求包装器.
	 */
	private static class RequestAttributeChangeIgnoringWrapper extends HttpServletRequestWrapper {

		public RequestAttributeChangeIgnoringWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
			// Ignore attribute change...
		}
	}

}
