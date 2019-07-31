package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * 创建{@code ContentNegotiationManager}并通过简单的setter配置一个或多个{@link ContentNegotiationStrategy}实例的工厂.
 * 下表显示了setter, 结果策略实例以及默认情况下是否正在使用:
 *
 * <table>
 * <tr>
 * <th>Property Setter</th>
 * <th>Underlying Strategy</th>
 * <th>Default Setting</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension}</td>
 * <td>{@link PathExtensionContentNegotiationStrategy Path Extension strategy}</td>
 * <td>On</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>{@link ParameterContentNegotiationStrategy Parameter strategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>{@link HeaderContentNegotiationStrategy Header strategy}</td>
 * <td>On</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>{@link FixedContentNegotiationStrategy Fixed content strategy}</td>
 * <td>Not set</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Not set</td>
 * </tr>
 * </table>
 *
 * <p>策略配置的顺序是固定的. Setter只能打开或关闭个别策略.
 * 如果需要自定义顺序, 只需直接实例化{@code ContentNegotiationManager}即可.
 *
 * <p>对于路径扩展和参数策略, 可以显式添加{@link #setMediaTypes MediaType 映射}.
 * 这将用于将路径扩展名或参数值(如 "json")解析为媒体类型 (如"application/json").
 *
 * <p>路径扩展名策略还将使用{@link ServletContext#getMimeType}和Java Activation框架 (JAF), 将路径扩展名解析为MediaType.
 * 可以{@link #setUseJaf 禁止}使用JAF.
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	private boolean favorPathExtension = true;

	private boolean favorParameter = false;

	private boolean ignoreAcceptHeader = false;

	private Map<String, MediaType> mediaTypes = new HashMap<String, MediaType>();

	private boolean ignoreUnknownPathExtensions = true;

	private Boolean useJaf;

	private String parameterName = "format";

	private ContentNegotiationStrategy defaultNegotiationStrategy;

	private ContentNegotiationManager contentNegotiationManager;

	private ServletContext servletContext;


	/**
	 * 是否应使用URL路径中的路径扩展名来确定所请求的媒体类型.
	 * <p>默认{@code true}, 在这种情况下, {@code /hotels.pdf}的请求将被解释为{@code "application/pdf"},
	 * 而不管'Accept' header.
	 */
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * 添加从路径扩展名或查询参数中提取的键到MediaType的映射.
	 * 这是参数策略工作所必需的.
	 * 此处显式注册的任何扩展名也列入白名单, 以用于Reflected File Download攻击检测
	 * (有关RFD攻击保护的更多详细信息, 请参阅Spring Framework参考文档).
	 * <p>路径扩展名策略还将尝试使用{@link ServletContext#getMimeType}和JAF来解析路径扩展名.
	 * 要更改此行为, 请参阅{@link #useJaf}属性.
	 * 
	 * @param mediaTypes 媒体类型映射
	 */
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			for (Map.Entry<Object, Object> entry : mediaTypes.entrySet()) {
				String extension = ((String)entry.getKey()).toLowerCase(Locale.ENGLISH);
				MediaType mediaType = MediaType.valueOf((String) entry.getValue());
				this.mediaTypes.put(extension, mediaType);
			}
		}
	}

	/**
	 * 用于Java代码的{@link #setMediaTypes}的替代方法.
	 */
	public void addMediaType(String fileExtension, MediaType mediaType) {
		this.mediaTypes.put(fileExtension, mediaType);
	}

	/**
	 * 用于Java代码的{@link #setMediaTypes}的替代方法.
	 */
	public void addMediaTypes(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
	}

	/**
	 * 是否忽略无法解析为任何媒体类型的路径扩展名请求.
	 * 如果没有匹配, 将此设置为{@code false}将导致{@code HttpMediaTypeNotAcceptableException}.
	 * <p>默认{@code true}.
	 */
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * 设置{@link #setFavorPathExtension favorPathExtension}时,
	 * 此属性确定是否允许使用JAF (Java Activation Framework)来解析特定MediaType的路径扩展名.
	 * <p>默认情况下, 未设置{@code PathExtensionContentNegotiationStrategy}将使用JAF.
	 */
	public void setUseJaf(boolean useJaf) {
		this.useJaf = useJaf;
	}

	private boolean isUseJafTurnedOff() {
		return (this.useJaf != null && !this.useJaf);
	}

	/**
	 * 是否应使用请求参数(默认"format")来确定所请求的媒体类型.
	 * 要使此选项起作用, 必须注册{@link #setMediaTypes 媒体类型映射}.
	 * <p>默认 {@code false}.
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * 设置{@link #setFavorParameter}打开时要使用的查询参数名称.
	 * <p>默认参数名称是{@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * 是否禁用检查 'Accept'请求 header.
	 * <p>默认{@code false}.
	 */
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * 设置在未请求内容类型时使用的默认内容类型.
	 * <p>默认无.
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * 设置自定义{@link ContentNegotiationStrategy}, 用于确定在未请求内容类型时要使用的内容类型.
	 * <p>默认无.
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * 由Spring调用以注入ServletContext.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<ContentNegotiationStrategy>();

		if (this.favorPathExtension) {
			PathExtensionContentNegotiationStrategy strategy;
			if (this.servletContext != null && !isUseJafTurnedOff()) {
				strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
			}
			else {
				strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
			}
			strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
			if (this.useJaf != null) {
				strategy.setUseJaf(this.useJaf);
			}
			strategies.add(strategy);
		}

		if (this.favorParameter) {
			ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
			strategy.setParameterName(this.parameterName);
			strategies.add(strategy);
		}

		if (!this.ignoreAcceptHeader) {
			strategies.add(new HeaderContentNegotiationStrategy());
		}

		if (this.defaultNegotiationStrategy != null) {
			strategies.add(this.defaultNegotiationStrategy);
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);
	}

	@Override
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
