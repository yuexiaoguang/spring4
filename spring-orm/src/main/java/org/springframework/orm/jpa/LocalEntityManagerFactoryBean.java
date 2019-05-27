package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;

/**
 * {@link org.springframework.beans.factory.FactoryBean},
 * 根据JPA的标准<i>独立</i>引导约定创建JPA {@link javax.persistence.EntityManagerFactory}.
 * 这是在Spring应用程序上下文中设置共享JPA EntityManagerFactory的最简单方法;
 * 然后可以通过依赖注入将EntityManagerFactory传递给基于JPA的DAO.
 * 请注意, 切换到JNDI查找或{@link LocalContainerEntityManagerFactoryBean}定义只是配置问题!
 *
 * <p>根据JPA独立引导程序约定, 配置通常从{@code META-INF/persistence.xml}配置文件中读取, 驻留在类路径中.
 * 此外, 大多数JPA提供者将需要一个特殊的VM代理 (在JVM启动时指定), 允许它们检测应用程序类.
 * 有关设置详细信息, 请参阅Java Persistence API规范和提供者文档.
 *
 * <p>此EntityManagerFactory引导程序适用于仅使用JPA进行数据访问的独立应用程序.
 * 如果要为外部DataSource和/或跨多个资源的全局事务设置持久化提供者,
 * 则需要将其部署到完整的Java EE应用程序服务器并通过JNDI访问已部署的EntityManagerFactory,
 * 或者根据JPA的容器约定, 使用Spring的{@link LocalContainerEntityManagerFactoryBean}和适当的配置进行本地设置.
 *
 * <p><b>Note:</b> 此FactoryBean在其能够传递给JPA提供者的配置方面具有有限的配置能力.
 * 如果您需要更灵活的配置, 例如将Spring管理的JDBC DataSource传递给JPA提供程序,
 * 请考虑使用Spring更强大的{@link LocalContainerEntityManagerFactoryBean}.
 *
 * <p><b>NOTE: 从Spring 4.0开始, Spring的JPA支持需要JPA 2.0或更高版本.</b>
 * 仍然支持基于JPA 1.0的应用程序; 但是, 在运行时需要一个兼容JPA 2.0/2.1的持久化提供者.
 */
@SuppressWarnings("serial")
public class LocalEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

	/**
	 * 初始化给定配置的EntityManagerFactory.
	 * 
	 * @throws javax.persistence.PersistenceException JPA初始化错误
	 */
	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		if (logger.isInfoEnabled()) {
			logger.info("Building JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		PersistenceProvider provider = getPersistenceProvider();
		if (provider != null) {
			// 直接通过PersistenceProvider创建EntityManagerFactory.
			EntityManagerFactory emf = provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
			if (emf == null) {
				throw new IllegalStateException(
						"PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
						getPersistenceUnitName() + "'");
			}
			return emf;
		}
		else {
			// 让JPA执行其标准的PersistenceProvider自动检测.
			return Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
		}
	}

}
