package org.springframework.core.io;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Resource}描述符的{@link java.beans.PropertyEditor Editor}, 自动转换{@code String}位置
 * e.g. {@code file:C:/myfile.txt}或{@code classpath:myfile.txt}到{@code Resource}属性,
 * 而不是使用{@code String}位置属性.
 *
 * <p>路径可能包含{@code ${...}}占位符, 需要解析为{@link org.springframework.core.env.Environment}属性:
 * e.g. {@code ${user.dir}}. 默认情况下会忽略无法解析的占位符.
 *
 * <p>委托给{@link ResourceLoader}进行繁重的工作, 默认情况下使用{@link DefaultResourceLoader}.
 */
public class ResourceEditor extends PropertyEditorSupport {

	private final ResourceLoader resourceLoader;

	private PropertyResolver propertyResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * 用{@link DefaultResourceLoader}和{@link StandardEnvironment}创建{@link ResourceEditor}类的新实例.
	 */
	public ResourceEditor() {
		this(new DefaultResourceLoader(), null);
	}

	/**
	 * @param resourceLoader 要使用的{@code ResourceLoader}
	 * @param propertyResolver 要使用的{@code PropertyResolver}
	 */
	public ResourceEditor(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
		this(resourceLoader, propertyResolver, true);
	}

	/**
	 * @param resourceLoader 要使用的{@code ResourceLoader}
	 * @param propertyResolver 要使用的{@code PropertyResolver}
	 * @param ignoreUnresolvablePlaceholders 如果在给定的{@code propertyResolver}中找不到相应的属性, 是否忽略无法解析的占位符
	 */
	public ResourceEditor(ResourceLoader resourceLoader, PropertyResolver propertyResolver,
			boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			String locationToUse = resolvePath(text).trim();
			setValue(this.resourceLoader.getResource(locationToUse));
		}
		else {
			setValue(null);
		}
	}

	/**
	 * 解析给定路径, 用{@code environment}中相应的属性值替换占位符.
	 * 
	 * @param path 原始文件路径
	 * 
	 * @return 解析后的文件路径
	 */
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			this.propertyResolver = new StandardEnvironment();
		}
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}


	@Override
	public String getAsText() {
		Resource value = (Resource) getValue();
		try {
			// 尝试确定资源的URL.
			return (value != null ? value.getURL().toExternalForm() : "");
		}
		catch (IOException ex) {
			// 无法确定资源URL - 返回null以指示没有适当的文本表示.
			return null;
		}
	}
}
