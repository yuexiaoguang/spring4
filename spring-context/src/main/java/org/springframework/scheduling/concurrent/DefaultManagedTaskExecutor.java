package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.Executor;
import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;

/**
 * 基于JNDI的{@link ConcurrentTaskExecutor}变体,
 * 在Java EE 7环境中对JSR-236的"java:comp/DefaultManagedExecutorService"执行默认查找.
 *
 * <p>Note: 这个类不是严格基于JSR-236的; 它可以与JNDI中可以找到的任何常规{@link java.util.concurrent.Executor}一起使用.
 * 实际适配{@link javax.enterprise.concurrent.ManagedExecutorService}发生在基类{@link ConcurrentTaskExecutor}本身.
 */
public class DefaultManagedTaskExecutor extends ConcurrentTaskExecutor implements InitializingBean {

	private JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	private String jndiName = "java:comp/DefaultManagedExecutorService";


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
	 * 设置查找是否发生在J2EE容器中, i.e. 如果JNDI名称尚未包含前缀"java:comp/env/", 则需要添加.
	 * PersistenceAnnotationBeanPostProcessor的默认值是 "true".
	 */
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * 指定要委派给{@link java.util.concurrent.Executor}的JNDI名称, 替换默认的JNDI名称"java:comp/DefaultManagedExecutorService".
	 * <p>如果"resourceRef"设置为"true", 则可以是完全限定的JNDI名称, 也可以是相对于当前环境命名上下文的JNDI名称.
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			setConcurrentExecutor(this.jndiLocator.lookup(this.jndiName, Executor.class));
		}
	}

}
