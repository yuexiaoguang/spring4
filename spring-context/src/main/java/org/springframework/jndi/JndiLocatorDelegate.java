package org.springframework.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.core.SpringProperties;

/**
 * {@link JndiLocatorSupport}具有公共查找方法的子类, 方便用作委托.
 */
public class JndiLocatorDelegate extends JndiLocatorSupport {

	/**
	 * 指示Spring忽略默认JNDI环境的系统属性,
	 * i.e. 始终从{@link #isDefaultJndiEnvironmentAvailable()}返回{@code false}.
	 * <p>默认 "false", 允许常规的默认JNDI访问 e.g. 在{@link JndiPropertySource}中.
	 * 将此标志切换为{@code true}, 是针对此类JNDI回退搜索开始时, 无法找到任何内容的情况的优化, 从而避免重复的JNDI查找开销.
	 * <p>请注意, 此标志仅影响JNDI回退搜索, 而不是显式配置的JNDI查找, 例如{@code DataSource}或其他一些环境资源.
	 * 该字段只会影响基于{@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()}检查尝试JNDI搜索的代码:
	 * 尤其是, {@code StandardServletEnvironment} 和 {@code StandardPortletEnvironment}.
	 */
	public static final String IGNORE_JNDI_PROPERTY_NAME = "spring.jndi.ignore";


	private static final boolean shouldIgnoreDefaultJndiEnvironment =
			SpringProperties.getFlag(IGNORE_JNDI_PROPERTY_NAME);


	@Override
	public Object lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName);
	}

	@Override
	public <T> T lookup(String jndiName, Class<T> requiredType) throws NamingException {
		return super.lookup(jndiName, requiredType);
	}


	/**
	 * 配置{@code JndiLocatorDelegate}, 并将其"resourceRef"属性设置为{@code true}, 这意味着所有名称都将以"java:comp/env/"作为前缀.
	 */
	public static JndiLocatorDelegate createDefaultResourceRefLocator() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		return jndiLocator;
	}

	/**
	 * 检查此JVM上是否有可用于J2EE环境的默认JNDI环境.
	 * 
	 * @return {@code true}如果可以使用默认的InitialContext, 否则{@code false}
	 */
	public static boolean isDefaultJndiEnvironmentAvailable() {
		if (shouldIgnoreDefaultJndiEnvironment) {
			return false;
		}
		try {
			new InitialContext().getEnvironment();
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
