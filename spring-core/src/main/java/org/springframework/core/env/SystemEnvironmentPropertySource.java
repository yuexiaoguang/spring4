package org.springframework.core.env;

import java.util.Map;

import org.springframework.util.Assert;

/**
 * 设计用于{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}的{@link MapPropertySource}的专业化.
 * 补偿Bash和其他不允许包含句点字符和/或连字符的变量的shell中的约束;
 * 还允许对属性名称进行大写变体, 以便更加惯用shell使用.
 *
 * <p>例如, 调用{@code getProperty("foo.bar")}将尝试查找原始属性或任何'等效'属性的值, 返回找到的第一个属性:
 * <ul>
 * <li>{@code foo.bar} - 原始名称</li>
 * <li>{@code foo_bar} - 带有下划线</li>
 * <li>{@code FOO.BAR} - 大写的原始名称</li>
 * <li>{@code FOO_BAR} - 大写的, 并带有下划线</li>
 * </ul>
 * 上述的任何连字符变体都可以使用, 甚至可以混合使用点/连字符变体.
 *
 * <p>这同样适用于对{@link #containsProperty(String)}的调用,
 * 如果存在上述任何属性, 则返回{@code true}, 否则{@code false}.
 *
 * <p>将活动的或默认的配置文件指定为环境变量时, 此功能特别有用.
 * 在Bash下不允许以下内容:
 *
 * <pre class="code">spring.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * 但是, 允许使用以下语法, 这也是更常规的:
 *
 * <pre class="code">SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>为此类(或包)启用调试或跟踪级别日志记录, 以获取解释何时发生这些“属性名称解析”的消息.
 *
 * <p>默认情况下, 此属性源包含在{@link StandardEnvironment}及其所有子类中.
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

	/**
	 * 使用给定名称创建一个新的{@code SystemEnvironmentPropertySource}, 并委托给给定的{@code MapPropertySource}.
	 */
	public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	/**
	 * 如果此属性源中存在具有给定名称或任何下划线/大写变体的属性, 则返回{@code true}.
	 */
	@Override
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 如果此属性源中存在具有给定名称或任何下划线/大写变体的属性, 则此实现将返回{@code true}.
	 */
	@Override
	public Object getProperty(String name) {
		String actualName = resolvePropertyName(name);
		if (logger.isDebugEnabled() && !name.equals(actualName)) {
			logger.debug("PropertySource '" + getName() + "' does not contain property '" + name +
					"', but found equivalent '" + actualName + "'");
		}
		return super.getProperty(actualName);
	}

	/**
	 * 检查此属性源是否包含具有给定名称的属性, 或其任何下划线/大写变体.
	 * 如果找到已解决的名称, 则返回已解析的名称, 否则返回原始名称. Never returns {@code null}.
	 */
	private String resolvePropertyName(String name) {
		Assert.notNull(name, "Property name must not be null");
		String resolvedName = checkPropertyName(name);
		if (resolvedName != null) {
			return resolvedName;
		}
		String uppercasedName = name.toUpperCase();
		if (!name.equals(uppercasedName)) {
			resolvedName = checkPropertyName(uppercasedName);
			if (resolvedName != null) {
				return resolvedName;
			}
		}
		return name;
	}

	private String checkPropertyName(String name) {
		// 按原样检查名称
		if (containsKey(name)) {
			return name;
		}
		// 检查替换点之后的名称
		String noDotName = name.replace('.', '_');
		if (!name.equals(noDotName) && containsKey(noDotName)) {
			return noDotName;
		}
		// 检查替换连字符之后的名称
		String noHyphenName = name.replace('-', '_');
		if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
			return noHyphenName;
		}
		// 检查替换点和连字符之后的名称
		String noDotNoHyphenName = noDotName.replace('-', '_');
		if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
			return noDotNoHyphenName;
		}
		// 放弃
		return null;
	}

	private boolean containsKey(String name) {
		return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
	}

	protected boolean isSecurityManagerPresent() {
		return (System.getSecurityManager() != null);
	}
}
