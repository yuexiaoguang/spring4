package org.springframework.jca.endpoint;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;

/**
 * 在Spring应用程序上下文中管理JCA 1.5消息端点的通用bean, 作为应用程序上下文生命周期的一部分激活和停用端点.
 *
 * <p>这个类是完全通用的, 它可以用于任何ResourceAdapter, 任何MessageEndpointFactory和任何ActivationSpec.
 * 它可以用标准bean样式配置, 例如通过Spring的XML bean定义格式, 如下所示:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 在此示例中, Spring自己的{@link GenericMessageEndpointFactory}
 * 用于指向恰好由指定目标ResourceAdapter支持的标准消息监听器对象:
 * 在这种情况下, ActiveMQ消息代理支持的JMS {@link javax.jms.MessageListener}对象, 定义为Spring bean:
 *
 * <pre class="code">
 * &lt;bean id="messageListener" class="com.myorg.messaging.myMessageListener"&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * 目标ResourceAdapter也可以配置为本地Spring bean (典型情况) 或从JNDI获取 (e.g. on WebLogic).
 * 对于上面的示例, 本地ResourceAdapter bean可以定义如下 (匹配上面的"resourceAdapter" bean引用):
 *
 * <pre class="code">
 * &lt;bean id="resourceAdapter" class="org.springframework.jca.support.ResourceAdapterFactoryBean"&gt;
 *   &lt;property name="resourceAdapter"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQResourceAdapter"&gt;
 *       &lt;property name="serverUrl" value="tcp://localhost:61616"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="workManager"&gt;
 *     &lt;bean class="org.springframework.jca.work.SimpleTaskWorkManager"/&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 对于不同的目标资源, 配置将简单地指向不同的ResourceAdapter和不同的ActivationSpec对象 (两者都特定于资源提供者),
 * 并且可能指向不同的消息监听器 (e.g. 基于JCA公共客户端接口的资源适配器的CCI {@link javax.resource.cci.MessageListener}).
 *
 * <p>可以通过ResourceAdapterFactoryBean上的"workManager"属性自定义异步执行策略 (如上所示).
 * 查看{@link org.springframework.jca.work.SimpleTaskWorkManager}的javadoc的配置选项;
 * 或者, 可以使用任何其他符合JCA标准的WorkManager (e.g. Geronimo's).
 *
 * <p>事务执行是具体消息端点的职责, 由指定的MessageEndpointFactory构建.
 * {@link GenericMessageEndpointFactory}通过其"transactionManager"属性支持XA事务参与,
 * 通常使用Spring {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 或在那里指定的普通{@link javax.transaction.TransactionManager}实现.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *       &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"/&gt;</pre>
 *
 * 或者, 检查资源提供者的ActivationSpec对象, 该对象应通过特定于提供者的配置标志支持本地事务,
 * e.g. ActiveMQActivationSpec的 "useRAManagedTransaction" bean属性.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *       &lt;property name="useRAManagedTransaction" value="true"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 */
public class GenericMessageEndpointManager implements SmartLifecycle, InitializingBean, DisposableBean {

	private ResourceAdapter resourceAdapter;

	private MessageEndpointFactory messageEndpointFactory;

	private ActivationSpec activationSpec;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	/**
	 * 设置管理端点的JCA ResourceAdapter.
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * 返回管理端点的JCA ResourceAdapter.
	 */
	public ResourceAdapter getResourceAdapter() {
		return this.resourceAdapter;
	}

	/**
	 * 设置要激活的JCA MessageEndpointFactory, 指向端点将委派给的MessageListener对象.
	 * <p>可以跨多个端点 (i.e. 多个GenericMessageEndpointManager实例)共享MessageEndpointFactory实例,
	 * 并应用了不同的{@link #setActivationSpec ActivationSpec}对象.
	 */
	public void setMessageEndpointFactory(MessageEndpointFactory messageEndpointFactory) {
		this.messageEndpointFactory = messageEndpointFactory;
	}

	/**
	 * 返回要激活的JCA MessageEndpointFactory.
	 */
	public MessageEndpointFactory getMessageEndpointFactory() {
		return this.messageEndpointFactory;
	}

	/**
	 * 设置用于激活端点的JCA ActivationSpec.
	 * <p>请注意, 不应在多个ResourceAdapter实例之间共享此ActivationSpec实例.
	 */
	public void setActivationSpec(ActivationSpec activationSpec) {
		this.activationSpec = activationSpec;
	}

	/**
	 * 返回用于激活端点的JCA ActivationSpec.
	 */
	public ActivationSpec getActivationSpec() {
		return this.activationSpec;
	}

	/**
	 * 设置是否在初始化此端点管理器并刷新上下文后, 自动启动端点激活.
	 * <p>默认"true". 关闭此标志以推迟端点激活, 直到显式调用{@link #start()}.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回'autoStartup'属性的值. 如果为"true", 则此端点管理器将从ContextRefreshedEvent启动.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定应启动和停止此端点管理器的阶段.
	 * 启动顺序从最低到最高, 关闭顺序与此相反.
	 * 默认情况下, 此值为Integer.MAX_VALUE, 表示此端点管理器尽可能晚地启动并尽快停止.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此端点管理器将启动和停止的阶段.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * 准备消息端点, 如果"autoStartup"标志设置为"true", 则自动激活它.
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (getResourceAdapter() == null) {
			throw new IllegalArgumentException("Property 'resourceAdapter' is required");
		}
		if (getMessageEndpointFactory() == null) {
			throw new IllegalArgumentException("Property 'messageEndpointFactory' is required");
		}
		ActivationSpec activationSpec = getActivationSpec();
		if (activationSpec == null) {
			throw new IllegalArgumentException("Property 'activationSpec' is required");
		}

		if (activationSpec.getResourceAdapter() == null) {
			activationSpec.setResourceAdapter(getResourceAdapter());
		}
		else if (activationSpec.getResourceAdapter() != getResourceAdapter()) {
			throw new IllegalArgumentException("ActivationSpec [" + activationSpec +
					"] is associated with a different ResourceAdapter: " + activationSpec.getResourceAdapter());
		}
	}

	/**
	 * 激活已配置的消息端点.
	 */
	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				try {
					getResourceAdapter().endpointActivation(getMessageEndpointFactory(), getActivationSpec());
				}
				catch (ResourceException ex) {
					throw new IllegalStateException("Could not activate message endpoint", ex);
				}
				this.running = true;
			}
		}
	}

	/**
	 * 停用已配置的消息端点.
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				getResourceAdapter().endpointDeactivation(getMessageEndpointFactory(), getActivationSpec());
				this.running = false;
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * 返回配置的消息端点当前是否处于活动状态.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 停用消息端点, 准备关闭.
	 */
	@Override
	public void destroy() {
		stop();
	}

}
