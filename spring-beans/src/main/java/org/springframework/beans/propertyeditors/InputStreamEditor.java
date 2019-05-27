package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;

/**
 * 单向PropertyEditor, 可以从文本字符串转换为{@code java.io.InputStream},
 * 将给定的String解释为Spring资源位置 (e.g. a URL String).
 *
 * <p>支持Spring样式的URL表示法: 任何完全合格的标准URL ("file:", "http:", etc.) 和Spring特殊的 "classpath:" pseudo-URL.
 *
 * <p>请注意, 这些流通常不会被Spring本身关闭!
 */
public class InputStreamEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用下面的默认ResourceEditor创建一个新的InputStreamEditor.
	 */
	public InputStreamEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用下面给定的ResourceEditor创建一个新的InputStreamEditor.
	 * 
	 * @param resourceEditor 要使用的ResourceEditor
	 */
	public InputStreamEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? resource.getInputStream() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to retrieve InputStream for " + resource, ex);
		}
	}

	/**
	 * 此实现返回{@code null}以指示没有适当的文本表示.
	 */
	@Override
	public String getAsText() {
		return null;
	}

}
