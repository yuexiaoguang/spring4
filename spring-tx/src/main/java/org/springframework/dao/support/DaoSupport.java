package org.springframework.dao.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * DAO的通用基类, 定义DAO初始化的模板方法.
 *
 * <p>由Spring的特定DAO支持类扩展, 例如: JdbcDaoSupport, JdoDaoSupport, etc.
 */
public abstract class DaoSupport implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		// 让抽象子类检查它们的配置.
		checkDaoConfig();

		// 让具体实现初始化自己.
		try {
			initDao();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of DAO failed", ex);
		}
	}

	/**
	 * 抽象子类必须覆盖它以检查其配置.
	 * <p>如果具体的子类本身不应覆盖此模板方法, 则应将实现者标记为{@code final}.
	 * 
	 * @throws IllegalArgumentException 在非法配置的情况下
	 */
	protected abstract void checkDaoConfig() throws IllegalArgumentException;

	/**
	 * 具体的子类可以覆盖此自定义初始化行为.
	 * 在此实例的bean属性的填充之后调用.
	 * 
	 * @throws Exception 如果DAO初始化失败 (将作为BeanInitializationException重新抛出)
	 */
	protected void initDao() throws Exception {
	}

}
