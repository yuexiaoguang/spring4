package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Class Classes}数组的属性编辑器, 用于启用 {@code Class[]}属性的直接填充, 而不必使用 {@code String}类名属性作为中间桥梁.
 *
 * <p>也支持"java.lang.String[]"格式的数组类名, 与标准 {@link Class#forName(String)}方法相反.
 */
public class ClassArrayEditor extends PropertyEditorSupport {

	private final ClassLoader classLoader;


	/**
	 * 使用线程上下文{@code ClassLoader}创建默认的{@code ClassEditor}.
	 */
	public ClassArrayEditor() {
		this(null);
	}

	/**
	 * 使用给定的{@code ClassLoader}创建默认的{@code ClassArrayEditor}.
	 * 
	 * @param classLoader 要使用的{@code ClassLoader}
	 * (或传递{@code null}作为线程上下文{@code ClassLoader})
	 */
	public ClassArrayEditor(ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
			Class<?>[] classes = new Class<?>[classNames.length];
			for (int i = 0; i < classNames.length; i++) {
				String className = classNames[i].trim();
				classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
			}
			setValue(classes);
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Class<?>[] classes = (Class[]) getValue();
		if (ObjectUtils.isEmpty(classes)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < classes.length; ++i) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(ClassUtils.getQualifiedName(classes[i]));
		}
		return sb.toString();
	}

}
