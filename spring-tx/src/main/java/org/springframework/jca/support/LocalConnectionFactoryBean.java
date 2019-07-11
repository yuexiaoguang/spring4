package org.springframework.jca.support;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link org.springframework.beans.factory.FactoryBean}以"非托管"模式创建本地JCA连接工厂
 * (由Java Connector Architecture规范定义).
 * 这是{@link org.springframework.jndi.JndiObjectFactoryBean}定义的直接替代,
 * 它从J2EE服务器的命名环境获取连接工厂句柄.
 *
 * <p>连接工厂的类型取决于实际的连接器:
 * 连接器可以公开其本机API (例如JDBC {@link javax.sql.DataSource}或JMS {@link javax.jms.ConnectionFactory})
 * 或遵循标准的Common Client Interface (CCI), 如JCA规范定义的那样.
 * CCI案例中的公开接口是{@link javax.resource.cci.ConnectionFactory}.
 *
 * <p>要使用此FactoryBean, 必须指定连接器的{@link #setManagedConnectionFactory "managedConnectionFactory"}
 * (通常配置为单独的JavaBean), 它将用于创建暴露给应用程序的实际连接工厂引用.
 * 或者, 也可以指定 {@link #setConnectionManager "connectionManager"}, 以便使用自定义ConnectionManager而不是连接器的默认值.
 *
 * <p><b>NOTE:</b> 在非托管模式下, 连接器未部署在应用程序服务器上, 或者更具体地未与应用程序服务器交互.
 * 因此, 它无法使用J2EE服务器的系统约定: 连接管理, 事务管理, 和安全管理.
 * 必须使用自定义ConnectionManager实现将这些服务与独立事务协调器等一起应用.
 *
 * <p>默认情况下, 连接器将使用本地ConnectionManager (包含在连接器中), 由于缺少XA登记, 它不能参与全局事务.
 * 需要指定支持XA的ConnectionManager才能使连接器与XA事务协调器进行交互.
 * 或者, 只需使用公开API的本地本地事务工具 (e.g. CCI 本地事务),
 * 或使用Spring的PlatformTransactionManager SPI的相应实现
 * (e.g. {@link org.springframework.jca.cci.connection.CciLocalTransactionManager})
 * 来驱动本地事务.
 */
public class LocalConnectionFactoryBean implements FactoryBean<Object>, InitializingBean {

	private ManagedConnectionFactory managedConnectionFactory;

	private ConnectionManager connectionManager;

	private Object connectionFactory;


	/**
	 * 设置应该用于创建所需连接工厂的JCA ManagerConnectionFactory.
	 * <p>ManagerConnectionFactory通常将设置为单独的bean (可能作为内部bean), 并使用JavaBean属性进行填充:
	 * 鼓励ManagerConnectionFactory遵循JCA规范的JavaBean模式, 类似于JDBC DataSource和JDO PersistenceManagerFactory.
	 * <p>请注意, ManagerConnectionFactory实现可能需要引用其JCA 1.5 ResourceAdapter,
	 * 通过{@link javax.resource.spi.ResourceAdapterAssociation}接口表示.
	 * 在将ManagerConnectionFactory传递给此LocalConnectionFactoryBean之前,
	 * 只需将相应的ResourceAdapter实例注入其"resourceAdapter" bean属性中.
	 */
	public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
		this.managedConnectionFactory = managedConnectionFactory;
	}

	/**
	 * 设置应该用于创建所需连接工厂的JCA ConnectionManager.
	 * <p>用于本地使用的ConnectionManager实现通常包含在JCA连接器中.
	 * 这样包含的ConnectionManager可能被设置为默认值, 无需显式指定.
	 */
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.managedConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'managedConnectionFactory' is required");
		}
		if (this.connectionManager != null) {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory(this.connectionManager);
		}
		else {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory();
		}
	}


	@Override
	public Object getObject() {
		return this.connectionFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.connectionFactory != null ? this.connectionFactory.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
