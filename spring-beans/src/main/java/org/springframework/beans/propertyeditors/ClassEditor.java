package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Class java.lang.Class}的属性编辑器, 启用{@code Class}属性的直接填充, 而无需使用String类名属性作为桥.
 *
 * <p>也支持 "java.lang.String[]"形式数组类名, 与标准{@link Class#forName(String)}方法相反.
 */
public class ClassEditor extends PropertyEditorSupport {

	private final ClassLoader classLoader;


	/**
	 * 使用线程上下文{@code ClassLoader}创建默认的{@code ClassEditor}.
	 */
	public ClassEditor() {
		this(null);
	}

	/**
	 * 使用给定的{@code ClassLoader}创建默认的{@code ClassArrayEditor}.
	 * 
	 * @param classLoader 要使用的{@code ClassLoader}
	 * (或{@code null}作为线程上下文{@code ClassLoader})
	 */
	public ClassEditor(ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Class<?> clazz = (Class<?>) getValue();
		if (clazz != null) {
			return ClassUtils.getQualifiedName(clazz);
		}
		else {
			return "";
		}
	}

}
