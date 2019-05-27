package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code java.net.URI}编辑器, 直接填充URI属性, 而不是使用String属性作为桥.
 *
 * <p>支持Spring风格的URI表示法:
 * 任何完全合格的标准URI ("file:", "http:", etc) 和Spring特殊的 "classpath:" pseudo-URL, 将解析为相应的URI.
 *
 * <p>默认情况下, 此编辑器会将字符串编码为URI.
 * 例如, 一个空格将被编码到 {@code %20}. 可以通过调用 {@link #URIEditor(boolean)}构造函数来更改此行为.
 *
 * <p>Note: URI比URL更宽松, 因为它不需要指定有效的协议.
 * 即使没有注册匹配的协议处理程序, 也允许有效URI语法中的任何模式.
 */
public class URIEditor extends PropertyEditorSupport {

	private final ClassLoader classLoader;

	private final boolean encode;



	/**
	 * 创建一个新的编码URIEditor, 将"classpath:"位置转换为标准URI (不试图将它们解析为物理资源).
	 */
	public URIEditor() {
		this(true);
	}

	/**
	 * 创建一个新的URIEditor, 将"classpath:"位置转换为标准URI(不试图将它们解析为物理资源).
	 * 
	 * @param encode 表示是否编码字符串
	 */
	public URIEditor(boolean encode) {
		this.classLoader = null;
		this.encode = encode;
	}

	/**
	 * 创建一个新的URIEditor, 使用给定的ClassLoader将 "classpath:"位置解析为物理资源URL.
	 * 
	 * @param classLoader 用于解析"classpath:"位置的ClassLoader (可能是{@code null}以指示默认的ClassLoader)
	 */
	public URIEditor(ClassLoader classLoader) {
		this(classLoader, true);
	}

	/**
	 * 创建一个新的URIEditor, 使用给定的ClassLoader将 "classpath:"位置解析为物理资源URL.
	 * 
	 * @param classLoader 用于解析"classpath:"位置的ClassLoader (可能是{@code null}以指示默认的ClassLoader)
	 * @param encode 表示是否编码字符串
	 */
	public URIEditor(ClassLoader classLoader, boolean encode) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.encode = encode;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String uri = text.trim();
			if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
				ClassPathResource resource = new ClassPathResource(
						uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
				try {
					setValue(resource.getURI());
				}
				catch (IOException ex) {
					throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
				}
			}
			else {
				try {
					setValue(createURI(uri));
				}
				catch (URISyntaxException ex) {
					throw new IllegalArgumentException("Invalid URI syntax: " + ex);
				}
			}
		}
		else {
			setValue(null);
		}
	}

	/**
	 * 为给定的用户指定的String值创建URI实例.
	 * <p>默认实现将值编码为符合RFC-2396的URI.
	 * 
	 * @param value 要转换为URI实例的值
	 * 
	 * @return URI实例
	 * @throws java.net.URISyntaxException 如果URI转换失败
	 */
	protected URI createURI(String value) throws URISyntaxException {
		int colonIndex = value.indexOf(':');
		if (this.encode && colonIndex != -1) {
			int fragmentIndex = value.indexOf('#', colonIndex + 1);
			String scheme = value.substring(0, colonIndex);
			String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
			String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
			return new URI(scheme, ssp, fragment);
		}
		else {
			// 不编码或值不包含任何模式 - 回退到默认的
			return new URI(value);
		}
	}


	@Override
	public String getAsText() {
		URI value = (URI) getValue();
		return (value != null ? value.toString() : "");
	}

}
