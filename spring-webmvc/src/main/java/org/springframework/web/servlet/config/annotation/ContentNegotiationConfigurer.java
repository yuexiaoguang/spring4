package org.springframework.web.servlet.config.annotation;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;

/**
 * 创建{@code ContentNegotiationManager}并使用一个或多个{@link ContentNegotiationStrategy}实例对其进行配置.
 *
 * <p>作为替代方案, 还可以依赖下面描述的一组默认值, 这些默认值可以打开或关闭, 也可以通过此构建器的方法自定义:
 *
 * <table>
 * <tr>
 * <th>Configurer Property</th>
 * <th>Underlying Strategy</th>
 * <th>Default Setting</th>
 * </tr>
 * <tr>
 * <td>{@link #favorPathExtension}</td>
 * <td>{@link PathExtensionContentNegotiationStrategy 路径扩展策略}</td>
 * <td>On</td>
 * </tr>
 * <tr>
 * <td>{@link #favorParameter}</td>
 * <td>{@link ParameterContentNegotiationStrategy 参数策略}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #ignoreAcceptHeader}</td>
 * <td>{@link HeaderContentNegotiationStrategy Header策略}</td>
 * <td>On</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentType}</td>
 * <td>{@link FixedContentNegotiationStrategy 固定内容策略}</td>
 * <td>Not set</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentTypeStrategy}</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Not set</td>
 * </tr>
 * </table>
 *
 * <p>策略配置的顺序是固定的. 只能打开或关闭它们.
 *
 * <p>对于路径扩展和参数策略, 可以显式添加{@link #mediaType MediaType映射}.
 * 这些将用于将路径扩展和/或查询参数值(如 "json")解析为具体的媒体类型, 例如"application/json".
 *
 * <p>路径扩展策略还将使用{@link ServletContext#getMimeType}和Java Activation框架 (JAF), 来解析MediaType的路径扩展.
 * 但是可以{@link #useJaf 禁止}使用JAF.
 */
public class ContentNegotiationConfigurer {

	private final ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();

	private final Map<String, MediaType> mediaTypes = new HashMap<String, MediaType>();


	public ContentNegotiationConfigurer(ServletContext servletContext) {
		this.factory.setServletContext(servletContext);
	}


	/**
	 * 是否应使用URL路径中的路径扩展来确定所请求的媒体类型.
	 * <p>默认为{@code true}, 在这种情况下, {@code /hotels.pdf}的请求
	 * 将被解释为{@code "application/pdf"}的请求, 而不管'Accept' header.
	 */
	public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
		this.factory.setFavorPathExtension(favorPathExtension);
		return this;
	}

	/**
	 * 添加从路径扩展或查询参数中提取的键到MediaType的映射.
	 * 这是参数策略所必需的.
	 * 此处明确注册的任何扩展名也列入白名单, 以用于反射文件下载攻击检测
	 * (有关RFD攻击保护的更多详细信息, 请参阅Spring Framework参考文档).
	 * <p>路径扩展策略还将尝试使用{@link ServletContext#getMimeType}和JAF来解析路径扩展.
	 * 要更改此行为, 请参阅{@link #useJaf}属性.
	 * 
	 * @param extension 要查找的键
	 * @param mediaType 媒体类型
	 */
	public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
		this.mediaTypes.put(extension, mediaType);
		return this;
	}

	/**
	 * {@link #mediaType}的替代方案.
	 */
	public ContentNegotiationConfigurer mediaTypes(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
		return this;
	}

	/**
	 * 类似于{@link #mediaType}, 但用于替换现有映射.
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		this.mediaTypes.clear();
		mediaTypes(mediaTypes);
		return this;
	}

	/**
	 * 是否忽略无法解析为任何媒体类型的路径扩展请求.
	 * 如果没有匹配, 将此设置为{@code false}将导致{@code HttpMediaTypeNotAcceptableException}.
	 * <p>默认为{@code true}.
	 */
	public ContentNegotiationConfigurer ignoreUnknownPathExtensions(boolean ignore) {
		this.factory.setIgnoreUnknownPathExtensions(ignore);
		return this;
	}

	/**
	 * 设置{@link #favorPathExtension}时,
	 * 此属性确定是否允许使用JAF (Java Activation Framework)来解析特定MediaType的路径扩展.
	 * <p>默认未设置, {@code PathExtensionContentNegotiationStrategy}将使用JAF.
	 */
	public ContentNegotiationConfigurer useJaf(boolean useJaf) {
		this.factory.setUseJaf(useJaf);
		return this;
	}

	/**
	 * 是否应使用请求参数 (默认为"format")来确定所请求的媒体类型.
	 * 要使此选项起作用, 必须注册{@link #mediaType(String, MediaType) 媒体类型映射}.
	 * <p>默认为{@code false}.
	 */
	public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
		this.factory.setFavorParameter(favorParameter);
		return this;
	}

	/**
	 * 设置{@link #favorParameter}打开时要使用的查询参数名称.
	 * <p>默认参数名称为{@code "format"}.
	 */
	public ContentNegotiationConfigurer parameterName(String parameterName) {
		this.factory.setParameterName(parameterName);
		return this;
	}

	/**
	 * 是否禁用检查'Accept'请求 header.
	 * <p>默认为{@code false}.
	 */
	public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
		return this;
	}

	/**
	 * 设置在未请求内容类型时使用的默认内容类型.
	 * <p>默认未设置.
	 */
	public ContentNegotiationConfigurer defaultContentType(MediaType defaultContentType) {
		this.factory.setDefaultContentType(defaultContentType);
		return this;
	}

	/**
	 * 设置自定义{@link ContentNegotiationStrategy}, 用于确定在未请求内容类型时要使用的内容类型.
	 * <p>默认未设置.
	 */
	public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
		this.factory.setDefaultContentTypeStrategy(defaultStrategy);
		return this;
	}


	/**
	 * 根据此配置器的设置构建{@link ContentNegotiationManager}.
	 */
	protected ContentNegotiationManager buildContentNegotiationManager() {
		this.factory.addMediaTypes(this.mediaTypes);
		this.factory.afterPropertiesSet();
		return this.factory.getObject();
	}

	/**
	 * @deprecated as of 4.3.12, in favor of {@link #buildContentNegotiationManager()}
	 */
	@Deprecated
	protected ContentNegotiationManager getContentNegotiationManager() throws Exception {
		return buildContentNegotiationManager();
	}

}
