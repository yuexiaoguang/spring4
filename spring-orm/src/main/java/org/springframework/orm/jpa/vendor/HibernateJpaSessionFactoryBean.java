package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 简单的{@code FactoryBean}, 公开了Hibernate支持的JPA {@link EntityManagerFactory}背后的{@link SessionFactory}.
 *
 * <p>主要用于通过{@link #setPersistenceUnitName "persistenceUnitName"} bean属性通过JPA持久化单元名称解析SessionFactory.
 *
 * <p>请注意, 对于简单的情况, 还可以简单地声明工厂方法:
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" factory-bean="entityManagerFactory" factory-method="getSessionFactory"/&gt;
 * </pre>
 *
 * <p>从JPA 2.1开始, {@link EntityManagerFactory#unwrap}也提供了一个很好的方法, 特别是在配置类安排中:
 *
 * <pre class="code">
 * &#064;Bean
 * public SessionFactory sessionFactory(@Qualifier("entityManagerFactory") EntityManagerFactory emf) {
 *     return emf.unwrap(SessionFactory.class);
 * }
 * </pre>
 *
 * 请注意: 由于Hibernate 5.2更改了其{@code SessionFactory}接口以扩展JPA的{@code EntityManagerFactory},
 * 因此在按类型注入时可能会出现冲突, 原始工厂和自定义{@code SessionFactory}都匹配{@code EntityManagerFactory}.
 * 这里建议使用原始工厂的显式限定符(如上所示).
 *
 * @deprecated 从Spring Framework 4.3.12开始, 针对Hibernate 5.2,
 * 支持基于{@link EntityManagerFactory#unwrap}的自定义解决方案, 带有显式限定符和/或主要标记
 */
@Deprecated
public class HibernateJpaSessionFactoryBean extends EntityManagerFactoryAccessor implements FactoryBean<SessionFactory> {

	@Override
	public SessionFactory getObject() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "EntityManagerFactory must not be null");
		try {
			Method getSessionFactory = emf.getClass().getMethod("getSessionFactory");
			return (SessionFactory) ReflectionUtils.invokeMethod(getSessionFactory, emf);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No compatible Hibernate EntityManagerFactory found: " + ex);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return SessionFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
