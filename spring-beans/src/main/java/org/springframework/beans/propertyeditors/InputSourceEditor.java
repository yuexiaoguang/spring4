package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;

/**
 * {@code org.xml.sax.InputSource}的编辑器, 从Spring资源位置字符串转换为SAX InputSource对象.
 *
 * <p>支持Spring样式的URL表示法: 任何完全合格的标准URL ("file:", "http:", etc) 和Spring特殊的 "classpath:" pseudo-URL.
 */
public class InputSourceEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用下面的默认ResourceEditor创建一个新的InputSourceEditor.
	 */
	public InputSourceEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用下面给定的ResourceEditor创建一个新的InputSourceEditor.
	 * 
	 * @param resourceEditor 要使用的ResourceEditor
	 */
	public InputSourceEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? new InputSource(resource.getURL().toString()) : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Could not retrieve URL for " + resource + ": " + ex.getMessage());
		}
	}

	@Override
	public String getAsText() {
		InputSource value = (InputSource) getValue();
		return (value != null ? value.getSystemId() : "");
	}

}
