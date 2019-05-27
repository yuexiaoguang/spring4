package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URL;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;

/**
 * {@code java.net.URL}的编辑器, 直接填充URL属性, 而不是使用String属性作为桥.
 *
 * <p>支持Spring样式的URL表示法:
 * 任何完全合格的标准URL ("file:", "http:", etc) 和Spring特殊的 "classpath:" pseudo-URL, 以及Spring的特定于上下文的相对文件路径.
 *
 * <p>Note: URL必须指定有效的协议, 否则它将被预先拒绝.
 * 但是在创建URL时, 目标资源不一定必须存在; 这取决于具体的资源类型.
 */
public class URLEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用下面默认的ResourceEditor创建一个新的URLEditor.
	 */
	public URLEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用下面给定的ResourceEditor创建一个新的URLEditor.
	 * 
	 * @param resourceEditor 要使用的ResourceEditor
	 */
	public URLEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? resource.getURL() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
		}
	}

	@Override
	public String getAsText() {
		URL value = (URL) getValue();
		return (value != null ? value.toExternalForm() : "");
	}

}
