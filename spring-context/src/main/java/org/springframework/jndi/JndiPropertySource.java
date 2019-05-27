package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.core.env.PropertySource;

/**
 * {@link PropertySource}实现, 从底层Spring {@link JndiLocatorDelegate}读取属性.
 *
 * <p>默认情况下, 底层{@code JndiLocatorDelegate}将配置,
 * 将{@link JndiLocatorDelegate#setResourceRef(boolean) "resourceRef"}属性设置为{@code true},
 * 意味着查找的名称将自动以"java:comp/env/"为前缀, 与已发布的
 * <a href="http://download.oracle.com/javase/jndi/tutorial/beyond/misc/policy.html">JNDI naming conventions</a>保持一致.
 * 要覆盖此设置或更改前缀, 请手动配置{@code JndiLocatorDelegate}, 并将其提供给此处接受它的构造函数之一.
 * 提供自定义JNDI属性时也是如此.
 * 在构建{@code JndiPropertySource}之前, 应使用{@link JndiLocatorDelegate#setJndiEnvironment(java.util.Properties)}指定这些内容.
 *
 * <p>请注意, {@link org.springframework.web.context.support.StandardServletEnvironment StandardServletEnvironment}
 * 默认包含{@code JndiPropertySource}, 并且底层{@link JndiLocatorDelegate}的任何自定义都可以在
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
 * 或{@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}中执行.
 */
public class JndiPropertySource extends PropertySource<JndiLocatorDelegate> {

	/**
	 * 创建一个具有给定名称的新{@code JndiPropertySource},
	 * 并将{@link JndiLocatorDelegate}配置为使用 "java:comp/env/"为任何名称添加前缀.
	 */
	public JndiPropertySource(String name) {
		this(name, JndiLocatorDelegate.createDefaultResourceRefLocator());
	}

	/**
	 * 使用给定名称和给定{@code JndiLocatorDelegate}.
	 */
	public JndiPropertySource(String name, JndiLocatorDelegate jndiLocator) {
		super(name, jndiLocator);
	}


	/**
	 * 此实现将查找并返回与底层{@link JndiLocatorDelegate}关联的给定名称的值.
	 * 如果在调用{@link JndiLocatorDelegate#lookup(String)}期间抛出{@link NamingException},
	 * 则返回{@code null}并发出带有异常消息的DEBUG级日志语句.
	 */
	@Override
	public Object getProperty(String name) {
		if (getSource().isResourceRef() && name.indexOf(':') != -1) {
			// 资源引用(前缀为 "java:comp/env") 模式.
			// 不要理会带有冒号的属性名称, 因为它们可能只包含一个默认值子句,
			// 即使在文本属性源中也不太可能匹配包括冒号部分,
			// 并且实际上从来没有意味着在JNDI中匹配那种方式, 其中冒号表示JNDI方案和实际名称之间的分隔符.
			return null;
		}

		try {
			Object value = this.source.lookup(name);
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] returned: [" + value + "]");
			}
			return value;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] threw NamingException " +
						"with message: " + ex.getMessage() + ". Returning null.");
			}
			return null;
		}
	}

}
