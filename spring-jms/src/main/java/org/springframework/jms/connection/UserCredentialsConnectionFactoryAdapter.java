package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于目标JMS {@link javax.jms.ConnectionFactory}的适配器, 将给定的用户凭据应用于每个标准{@code createConnection()}调用,
 * 即隐式调用目标上的{@code createConnection(username, password)}.
 * 所有其他方法只是委托给目标ConnectionFactory的相应方法.
 *
 * <p>可用于代理未配置用户凭据的目标JNDI ConnectionFactory.
 * 客户端代码可以与ConnectionFactory一起使用, 而无需在每个{@code createConnection()}调用上传递用户名和密码.
 *
 * <p>在以下示例中, 客户端代码可以简单地透明地使用预配置的"myConnectionFactory",
 * 使用指定的用户凭据隐式访问"myTargetConnectionFactory".
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jms/mycf"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean></pre>
 *
 * <p>如果"username"为空, 则此代理将简单地委托给目标ConnectionFactory的标准{@code createConnection()}方法.
 * 这可以用于保持UserCredentialsConnectionFactoryAdapter bean定义
 * 仅用于在特定目标ConnectionFactory需要时隐式传入用户凭据的<i>选项</i>.
 */
public class UserCredentialsConnectionFactoryAdapter
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	private ConnectionFactory targetConnectionFactory;

	private String username;

	private String password;

	private final ThreadLocal<JmsUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<JmsUserCredentials>("Current JMS user credentials");


	/**
	 * 设置此ConnectionFactory应委托给的目标ConnectionFactory.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 设置此适配器用于检索Connection的用户名.
	 * 默认没有用户名.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 设置此适配器用于检索Connection的密码.
	 * 默认没有密码.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	/**
	 * 为此代理和当前线程设置用户凭据.
	 * 给定的用户名和密码将应用于此ConnectionFactory代理上的所有后续{@code createConnection()}调用.
	 * <p>这将覆盖任何静态指定的用户凭据, 即"username" 和 "password" bean属性的值.
	 * 
	 * @param username 要应用的用户名
	 * @param password 要应用的密码
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JmsUserCredentials(username, password));
	}

	/**
	 * 从当前线程中删除此代理的所有用户凭据.
	 * 之后将再次应用静态指定的用户凭据.
	 */
	public void removeCredentialsFromCurrentThread() {
		this.threadBoundCredentials.remove();
	}


	/**
	 * 确定当前是否存在线程绑定凭据, 如果可用则使用它们, 否则回退到静态指定的用户名和密码 (i.e. bean属性的值).
	 */
	@Override
	public final Connection createConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标ConnectionFactory.
	 */
	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return doCreateConnection(username, password);
	}

	/**
	 * 此实现委托给目标ConnectionFactory的{@code createConnection(username, password)}方法, 传入指定的用户凭据.
	 * 如果指定的用户名为空, 则它将简单地委托给目标ConnectionFactory的标准{@code createConnection()}方法.
	 * 
	 * @param username 要使用的用户名
	 * @param password 要使用的密码
	 * 
	 * @return the Connection
	 */
	protected Connection doCreateConnection(String username, String password) throws JMSException {
		Assert.state(this.targetConnectionFactory != null, "'targetConnectionFactory' is required");
		if (StringUtils.hasLength(username)) {
			return this.targetConnectionFactory.createConnection(username, password);
		}
		else {
			return this.targetConnectionFactory.createConnection();
		}
	}


	/**
	 * 确定当前是否存在线程绑定的凭据, 如果可用则使用它们, 否则回退到静态指定的用户名和密码 (i.e. bean属性的值).
	 */
	@Override
	public final QueueConnection createQueueConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateQueueConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateQueueConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标QueueConnectionFactory.
	 */
	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		return doCreateQueueConnection(username, password);
	}

	/**
	 * 此实现委托给目标QueueConnectionFactory的{@code createQueueConnection(username, password)}方法, 传入指定的用户凭据.
	 * 如果指定的用户名为空, 则它将简单地委托给目标ConnectionFactory的标准{@code createQueueConnection()}方法.
	 * 
	 * @param username 要使用的用户名
	 * @param password 要使用的密码
	 * 
	 * @return the Connection
	 */
	protected QueueConnection doCreateQueueConnection(String username, String password) throws JMSException {
		Assert.state(this.targetConnectionFactory != null, "'targetConnectionFactory' is required");
		if (!(this.targetConnectionFactory instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
		}
		QueueConnectionFactory queueFactory = (QueueConnectionFactory) this.targetConnectionFactory;
		if (StringUtils.hasLength(username)) {
			return queueFactory.createQueueConnection(username, password);
		}
		else {
			return queueFactory.createQueueConnection();
		}
	}


	/**
	 * 确定当前是否存在线程绑定的凭据, 如果可用则使用它们, 回退到静态指定的用户名和密码 (i.e. bean属性的值).
	 */
	@Override
	public final TopicConnection createTopicConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateTopicConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateTopicConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标TopicConnectionFactory.
	 */
	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		return doCreateTopicConnection(username, password);
	}

	/**
	 * 此实现委托给目标TopicConnectionFactory的{@code createTopicConnection(username, password)}方法, 传入指定的用户凭据.
	 * 如果指定的用户名为空, 则它将简单地委托给目标ConnectionFactory的标准{@code createTopicConnection()}方法.
	 * 
	 * @param username 要使用的用户名
	 * @param password 要使用的密码
	 * 
	 * @return the Connection
	 */
	protected TopicConnection doCreateTopicConnection(String username, String password) throws JMSException {
		Assert.state(this.targetConnectionFactory != null, "'targetConnectionFactory' is required");
		if (!(this.targetConnectionFactory instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
		}
		TopicConnectionFactory queueFactory = (TopicConnectionFactory) this.targetConnectionFactory;
		if (StringUtils.hasLength(username)) {
			return queueFactory.createTopicConnection(username, password);
		}
		else {
			return queueFactory.createTopicConnection();
		}
	}


	/**
	 * 用作ThreadLocal值的内部类.
	 */
	private static class JmsUserCredentials {

		public final String username;

		public final String password;

		private JmsUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JmsUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}
}
