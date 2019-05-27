package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;

/**
 * 基于JNDI的{@link CustomizableThreadFactory}变体,
 * 在Java EE 7环境中对JSR-236的"java:comp/DefaultManagedThreadFactory"执行默认查找;
 * 如果找不到, 则回退到本地{@link CustomizableThreadFactory}设置.
 *
 * <p>这是在Java EE 7环境中运行时使用托管线程的便捷方式,
 * 只需使用常规本地线程 - 无需条件设置 (i.e. 没有配置文件).
 *
 * <p>Note: 这个类不是严格基于JSR-236的; 它可以与JNDI中可以找到的任何常规{@link java.util.concurrent.ThreadFactory}一起使用.
 * 因此, 可以通过{@link #setJndiName "jndiName"} bean属性自定义默认的JNDI名称"java:comp/DefaultManagedThreadFactory".
 */
@SuppressWarnings("serial")
public class DefaultManagedAwareThreadFactory extends CustomizableThreadFactory implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	private String jndiName = "java:comp/DefaultManagedThreadFactory";

	private ThreadFactory threadFactory;


	/**
	 * 设置用于JNDI查找的JNDI模板.
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiLocator.setJndiTemplate(jndiTemplate);
	}

	/**
	 * 设置用于JNDI查找的JNDI环境.
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiLocator.setJndiEnvironment(jndiEnvironment);
	}

	/**
	 * 设置查找是否发生在J2EE容器中,
	 * i.e. 如果JNDI名称尚未包含前缀"java:comp/env/", 则需要添加.
	 * PersistenceAnnotationBeanPostProcessor的默认值是 "true".
	 */
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * 指定要委托给的{@link java.util.concurrent.ThreadFactory}的JNDI名称,
	 * 替换默认的JNDI名称"java:comp/DefaultManagedThreadFactory".
	 * <p>如果"resourceRef"设置为"true", 则可以是完全限定的JNDI名称, 也可以是相对于当前环境命名上下文的JNDI名称.
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			try {
				this.threadFactory = this.jndiLocator.lookup(this.jndiName, ThreadFactory.class);
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to retrieve [" + this.jndiName + "] from JNDI", ex);
				}
				logger.info("Could not find default managed thread factory in JNDI - " +
						"proceeding with default local thread factory");
			}
		}
	}


	@Override
	public Thread newThread(Runnable runnable) {
		if (this.threadFactory != null) {
			return this.threadFactory.newThread(runnable);
		}
		else {
			return super.newThread(runnable);
		}
	}

}
