package org.springframework.jndi;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JNDI访问器的超类, 提供"jndiTemplate"和"jndiEnvironment" bean属性.
 */
public class JndiAccessor {

	/**
	 * Logger, available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private JndiTemplate jndiTemplate = new JndiTemplate();


	/**
	 * 设置JNDI模板以用于JNDI查找.
	 * <p>还可以通过"jndiEnvironment"指定JNDI环境设置.
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * 返回JNDI模板以用于JNDI查找.
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * 设置JNDI环境以用于JNDI查找.
	 * <p>使用给定的环境设置创建JndiTemplate.
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 返回JNDI环境以用于JNDI查找.
	 */
	public Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}

}
