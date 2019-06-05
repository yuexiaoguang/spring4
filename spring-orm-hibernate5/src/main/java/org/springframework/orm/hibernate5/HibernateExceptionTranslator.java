package org.springframework.orm.hibernate5;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * {@link PersistenceExceptionTranslator}能够将{@link HibernateException}实例转换为Spring的{@link DataAccessException}层次结构.
 * 从Spring 4.3.2和Hibernate 5.2开始, 它还转换了标准的JPA {@link PersistenceException}实例.
 *
 * <p>由{@link LocalSessionFactoryBean}扩展, 因此除了{@code LocalSessionFactoryBean}之外, 无需声明此转换器.
 *
 * <p>使用{@code @Configuration}类配置容器时, 必须手动注册此类型的{@code @Bean}.
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException) {
			if (ex.getCause() instanceof HibernateException) {
				return convertHibernateAccessException((HibernateException) ex.getCause());
			}
			return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
		}
		return null;
	}

	/**
	 * 将给定的HibernateException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * 
	 * @param ex 发生的HibernateException
	 * 
	 * @return 相应的DataAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

}
