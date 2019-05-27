package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * {@link Properties}对象的自定义{@link java.beans.PropertyEditor}.
 *
 * <p>处理从内容{@link String}到{@code Properties}对象的转换.
 * 还处理{@link Map}到{@code Properties}转换, 用于通过XML "map"条目填充 {@code Properties}对象.
 *
 * <p>所需格式在标准{@code Properties}文档中定义. 每个属性必须在新行上.
 */
public class PropertiesEditor extends PropertyEditorSupport {

	/**
	 * 将{@link String}转换为{@link Properties}, 将其视为属性内容.
	 * 
	 * @param text 要转换的文本
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		Properties props = new Properties();
		if (text != null) {
			try {
				// 必须使用ISO-8859-1编码, 因为Properties.load(stream)需要它.
				props.load(new ByteArrayInputStream(text.getBytes("ISO-8859-1")));
			}
			catch (IOException ex) {
				// Should never happen.
				throw new IllegalArgumentException(
						"Failed to parse [" + text + "] into Properties", ex);
			}
		}
		setValue(props);
	}

	/**
	 * 按原样获取{@link Properties}; 将{@link Map}转换为{@code Properties}.
	 */
	@Override
	public void setValue(Object value) {
		if (!(value instanceof Properties) && value instanceof Map) {
			Properties props = new Properties();
			props.putAll((Map<?, ?>) value);
			super.setValue(props);
		}
		else {
			super.setValue(value);
		}
	}

}
