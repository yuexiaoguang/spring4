package org.springframework.jca.cci.connection;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

/**
 * 目标CCI {@link javax.resource.cci.ConnectionFactory}的适配器,
 * 将给定的ConnectionSpec应用于每个标准{@code getConnection()}调用,
 * 即在目标上隐式调用{@code getConnection(ConnectionSpec)}.
 * 所有其他方法只是委托给目标ConnectionFactory的相应方法.
 *
 * <p>可用于代理未配置ConnectionSpec的目标JNDI ConnectionFactory.
 * 客户端代码可以与ConnectionFactory一起使用, 而无需在每个{@code getConnection()}调用上传入ConnectionSpec.
 *
 * <p>在以下示例中, 客户端代码可以简单地透明地使用预配置的"myConnectionFactory",
 * 使用指定的用户凭据隐式访问"myTargetConnectionFactory".
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/cci/mycf"/&gt;
 * &lt;/bean>
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jca.cci.connection.ConnectionSpecConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="connectionSpec"&gt;
 *     &lt;bean class="your.resource.adapter.ConnectionSpecImpl"&gt;
 *       &lt;property name="username" value="myusername"/&gt;
 *       &lt;property name="password" value="mypassword"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果"connectionSpec"为空, 则此代理将简单地委托给目标ConnectionFactory的标准{@code getConnection()}方法.
 * 这可以用于保持UserCredentialsConnectionFactoryAdapter bean定义仅针对隐式传入ConnectionSpec的<i>选项</i>,
 * 如果特定目标ConnectionFactory需要它.
 */
@SuppressWarnings("serial")
public class ConnectionSpecConnectionFactoryAdapter extends DelegatingConnectionFactory {

	private ConnectionSpec connectionSpec;

	private final ThreadLocal<ConnectionSpec> threadBoundSpec =
			new NamedThreadLocal<ConnectionSpec>("Current CCI ConnectionSpec");


	/**
	 * 设置此适配器应用于检索Connections的ConnectionSpec.
	 * 默认无.
	 */
	public void setConnectionSpec(ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * 为此代理和当前线程设置ConnectionSpec.
	 * 给定的ConnectionSpec将应用于此ConnectionFactory代理上的所有后续{@code getConnection()}调用.
	 * <p>这将覆盖任何静态指定的"connectionSpec"属性.
	 * 
	 * @param spec 要应用的ConnectionSpec
	 */
	public void setConnectionSpecForCurrentThread(ConnectionSpec spec) {
		this.threadBoundSpec.set(spec);
	}

	/**
	 * 从当前线程中删除此代理的任何ConnectionSpec.
	 * 之后再次应用静态指定的ConnectionSpec.
	 */
	public void removeConnectionSpecFromCurrentThread() {
		this.threadBoundSpec.remove();
	}


	/**
	 * 确定当前是否存在线程绑定的ConnectionSpec, 如果可用则使用它, 否则回退到静态指定的"connectionSpec"属性.
	 */
	@Override
	public final Connection getConnection() throws ResourceException {
		ConnectionSpec threadSpec = this.threadBoundSpec.get();
		if (threadSpec != null) {
			return doGetConnection(threadSpec);
		}
		else {
			return doGetConnection(this.connectionSpec);
		}
	}

	/**
	 * 此实现委托给目标ConnectionFactory的{@code getConnection(ConnectionSpec)}方法, 传入指定的用户凭据.
	 * 如果指定的用户名为空, 则它将简单地委托给目标ConnectionFactory的标准 {@code getConnection()}方法.
	 * 
	 * @param spec 要应用的ConnectionSpec
	 * 
	 * @return Connection
	 */
	protected Connection doGetConnection(ConnectionSpec spec) throws ResourceException {
		ConnectionFactory connectionFactory = getTargetConnectionFactory();
		Assert.state(connectionFactory != null, "No 'targetConnectionFactory' set");
		return (spec != null ? connectionFactory.getConnection(spec) : connectionFactory.getConnection());
	}
}
