package org.springframework.orm.hibernate5.support;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.hibernate5.HibernateTemplate;

/**
 * 基于Hibernate的数据访问对象的便捷超类.
 *
 * <p>需要设置{@link SessionFactory}, 通过{@link #getHibernateTemplate()}方法基于它将
 * {@link org.springframework.orm.hibernate5.HibernateTemplate}提供给子类.
 * 也可以直接用HibernateTemplate初始化, 以便重用后者的设置, 如SessionFactory, 异常转换, 刷新模式等.
 *
 * <p>如果传入SessionFactory, 该类将创建自己的HibernateTemplate实例.
 * 默认情况下, HibernateTemplate上的"allowCreate"标志将为"true".
 * 可以通过覆盖{@link #createHibernateTemplate}来使用自定义的HibernateTemplate实例.
 *
 * <p><b>NOTE: Hibernate访问代码也可以用简单的Hibernate风格编码.
 * 因此, 对于新启动的项目, 请考虑采用标准的Hibernate风格的编码数据访问对象, 基于{@link SessionFactory#getCurrentSession()}.
 * 这个HibernateTemplate主要作为基于Hibernate 3的数据访问代码的迁移帮助类而存在, 可以从Hibernate 5.x中的错误修复中受益.</b>
 */
public abstract class HibernateDaoSupport extends DaoSupport {

	private HibernateTemplate hibernateTemplate;


	/**
	 * 设置此DAO使用的Hibernate SessionFactory.
	 * 将自动为给定的SessionFactory创建一个HibernateTemplate.
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
			this.hibernateTemplate = createHibernateTemplate(sessionFactory);
		}
	}

	/**
	 * 仅在使用SessionFactory引用填充DAO时才调用!
	 * <p>可以在子类中重写以提供具有不同配置的HibernateTemplate实例, 或者自定义HibernateTemplate子类.
	 * 
	 * @param sessionFactory 用于创建HibernateTemplate的Hibernate SessionFactory
	 * 
	 * @return 新的HibernateTemplate实例
	 */
	protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
		return new HibernateTemplate(sessionFactory);
	}

	/**
	 * 返回此DAO使用的Hibernate SessionFactory.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * 显式设置此DAO的HibernateTemplate, 作为指定SessionFactory的替代方法.
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * 返回此DAO的HibernateTemplate, 使用SessionFactory预先初始化或显式设置.
	 * <p><b>Note: 返回的HibernateTemplate是一个共享实例.</b>
	 * 可以内省其配置, 但不能修改配置 (除了{@link #initDao}实现之外).
	 * 考虑通过{@code new HibernateTemplate(getSessionFactory())}创建自定义HibernateTemplate实例,
	 * 在这种情况下, 可以自定义生成的实例上的设置.
	 */
	public final HibernateTemplate getHibernateTemplate() {
	  return this.hibernateTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("'sessionFactory' or 'hibernateTemplate' is required");
		}
	}


	/**
	 * 方便地获取当前的Hibernate Session.
	 * 
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException 如果无法创建会话
	 */
	protected final Session currentSession() throws DataAccessResourceFailureException {
		return getSessionFactory().getCurrentSession();
	}

}
