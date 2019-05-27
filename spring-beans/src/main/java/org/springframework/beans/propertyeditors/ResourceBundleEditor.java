package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 标准JDK的{@link java.util.ResourceBundle ResourceBundles}的{@link java.beans.PropertyEditor}实现.
 *
 * <p>仅支持从String转换, 但不支持到String转换.
 *
 * 下面是一些使用基于XML的元数据在(正确配置的)Spring容器中使用此类的示例:
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'messages' property is of type java.util.ResourceBundle.
 *        the 'DialogMessages.properties' file exists at the root of the CLASSPATH
 *    --&gt;
 *    &lt;property name="messages" value="DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'DialogMessages.properties' file exists in the 'com/messages' package
 *    --&gt;
 *    &lt;property name="messages" value="com/messages/DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>'正确配置'的 Spring {@link org.springframework.context.ApplicationContext container}
 * 可能包含 {@link org.springframework.beans.factory.config.CustomEditorConfigurer}定义, 这样可以透明地实现转换:
 *
 * <pre class="code"> &lt;bean class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *    &lt;property name="customEditors"&gt;
 *        &lt;map&gt;
 *            &lt;entry key="java.util.ResourceBundle"&gt;
 *                &lt;bean class="org.springframework.beans.propertyeditors.ResourceBundleEditor"/&gt;
 *            &lt;/entry&gt;
 *        &lt;/map&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>请注意, 默认情况下, 此{{link java.beans.PropertyEditor}未注册任何Spring基础结构.
 *
 * <p>感谢David Leal Valmana提出的建议和初始原型.
 */
public class ResourceBundleEditor extends PropertyEditorSupport {

	/**
	 * 用于在{@link #setAsText(String) 从String转换}时, 区分基本名称和语言环境的分隔符.
	 */
	public static final String BASE_NAME_SEPARATOR = "_";


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		Assert.hasText(text, "'text' must not be empty");
		String name = text.trim();

		int separator = name.indexOf(BASE_NAME_SEPARATOR);
		if (separator == -1) {
			setValue(ResourceBundle.getBundle(name));
		}
		else {
			// 该名称可能包含区域设置信息
			String baseName = name.substring(0, separator);
			if (!StringUtils.hasText(baseName)) {
				throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
			}
			String localeString = name.substring(separator + 1);
			Locale locale = StringUtils.parseLocaleString(localeString);
			setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
		}
	}

}
