package org.springframework.beans;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;

/**
 * {@link PropertyValues}对象的{@link java.beans.PropertyEditor Editor}.
 *
 * <p>所需格式在{@link java.util.Properties}文档中定义. 每个属性必须在新行上.
 *
 * <p>目前的实现依赖于下面的{@link org.springframework.beans.propertyeditors.PropertiesEditor}.
 */
public class PropertyValuesEditor extends PropertyEditorSupport {

	private final PropertiesEditor propertiesEditor = new PropertiesEditor();

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.propertiesEditor.setAsText(text);
		Properties props = (Properties) this.propertiesEditor.getValue();
		setValue(new MutablePropertyValues(props));
	}

}

