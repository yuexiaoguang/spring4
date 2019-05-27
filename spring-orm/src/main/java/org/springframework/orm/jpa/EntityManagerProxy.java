package org.springframework.orm.jpa;

import javax.persistence.EntityManager;

/**
 * 由EntityManager代理实现的{@link javax.persistence.EntityManager}的子接口.
 * 允许访问底层目标EntityManager.
 *
 * <p>此接口主要用于框架使用.
 * 应用程序代码应该更喜欢使用{@link javax.persistence.EntityManager#getDelegate()}方法来访问底层资源的本机功能.
 */
public interface EntityManagerProxy extends EntityManager {

	/**
	 * 返回此代理将委派给的底层EntityManager.
	 * <p>对于扩展的EntityManager, 这将是关联的原始EntityManager.
	 * <p>在共享("transactional") EntityManager的情况下, 这将是当前与事务关联的原始EntityManager.
	 * 在事务之外, 将抛出IllegalStateException.
	 * 
	 * @return 底层原始EntityManager (never {@code null})
	 * @throws IllegalStateException 如果没有底层的EntityManager可用
	 */
	EntityManager getTargetEntityManager() throws IllegalStateException;

}
