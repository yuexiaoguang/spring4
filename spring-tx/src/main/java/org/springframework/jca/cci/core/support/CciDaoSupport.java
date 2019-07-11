package org.springframework.jca.cci.core.support;

import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.springframework.dao.support.DaoSupport;
import org.springframework.jca.cci.CannotGetCciConnectionException;
import org.springframework.jca.cci.connection.ConnectionFactoryUtils;
import org.springframework.jca.cci.core.CciTemplate;

/**
 * 基于CCI的数据访问对象的便捷超类.
 *
 * <p>需要设置{@link javax.resource.cci.ConnectionFactory},
 * 通过{@link #getCciTemplate()}方法将{@link org.springframework.jca.cci.core.CciTemplate}基于它提供给子类.
 *
 * <p>此基类主要用于CciTemplate用法, 但也可以在直接使用Connection或使用{@code org.springframework.jca.cci.object}类时使用.
 */
public abstract class CciDaoSupport extends DaoSupport {

	private CciTemplate cciTemplate;


	/**
	 * 设置此DAO使用的ConnectionFactory.
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		if (this.cciTemplate == null || connectionFactory != this.cciTemplate.getConnectionFactory()) {
		  this.cciTemplate = createCciTemplate(connectionFactory);
		}
	}

	/**
	 * 为给定的ConnectionFactory创建一个CciTemplate.
	 * 仅在使用ConnectionFactory引用填充DAO时调用!
	 * <p>可以在子类中重写以提供具有不同配置的CciTemplate实例, 或者自定义CciTemplate子类.
	 * 
	 * @param connectionFactory 用于创建CciTemplate的CCI ConnectionFactory
	 * 
	 * @return 新的CciTemplate实例
	 */
	protected CciTemplate createCciTemplate(ConnectionFactory connectionFactory) {
		return new CciTemplate(connectionFactory);
	}

	/**
	 * 返回此DAO使用的ConnectionFactory.
	 */
	public final ConnectionFactory getConnectionFactory() {
		return this.cciTemplate.getConnectionFactory();
	}

	/**
	 * 显式设置此DAO的CciTemplate, 作为指定ConnectionFactory的替代方法.
	 */
	public final void setCciTemplate(CciTemplate cciTemplate) {
		this.cciTemplate = cciTemplate;
	}

	/**
	 * 返回此DAO的CciTemplate, 使用ConnectionFactory预先初始化或显式设置.
	 */
	public final CciTemplate getCciTemplate() {
	  return this.cciTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.cciTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'cciTemplate' is required");
		}
	}


	/**
	 * 获取从主模板实例派生的CciTemplate, 继承ConnectionFactory和其他设置,
	 * 但覆盖用于获取Connections的ConnectionSpec.
	 * 
	 * @param connectionSpec 返回的模板实例应该获取连接的CCI ConnectionSpec
	 * 
	 * @return 派生的模板实例
	 */
	protected final CciTemplate getCciTemplate(ConnectionSpec connectionSpec) {
		return getCciTemplate().getDerivedTemplate(connectionSpec);
	}

	/**
	 * 从当前事务或新事务获取CCI连接.
	 * 
	 * @return the CCI Connection
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果尝试获取连接失败
	 */
	protected final Connection getConnection() throws CannotGetCciConnectionException {
		return ConnectionFactoryUtils.getConnection(getConnectionFactory());
	}

	/**
	 * 如果未绑定到线程, 则关闭通过此Bean的ConnectionFactory创建的给定CCI连接.
	 * 
	 * @param con 要关闭的Connection
	 */
	protected final void releaseConnection(Connection con) {
		ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
	}

}
