package org.springframework.orm.jpa;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * SPI接口, 允许将特定于供应商的行为插入到Spring的EntityManagerFactory创建器中.
 * 用作所有特定于供应商的属性的单个配置点.
 */
public interface JpaVendorAdapter {

	/**
	 * 返回特定于供应商的持久化提供者.
	 */
	PersistenceProvider getPersistenceProvider();

	/**
	 * 返回持久化提供者的根包的名称 (e.g. "oracle.toplink.essentials").
	 * 将用于从临时类重写中排除提供者类.
	 */
	String getPersistenceProviderRootPackage();

	/**
	 * 返回给定持久性单元的特定于供应商的JPA属性的Map, 通常基于此JpaVendorAdapter实例中的设置.
	 * <p>请注意, 可能在EntityManagerFactory bean上定义了更多JPA属性, 这可能会覆盖此处指定的各个JPA属性值.
	 * <p>对于非依赖于单元的属性, 此实现委托给{@link #getJpaPropertyMap()}.
	 * 实际上, 只有在实际需要对特定于单元的特征(如事务类型)作出反应时, 才需要实现此基于PersistenceUnitInfo的变体.
	 * <p><b>NOTE:</b> 仅在Java EE样式容器引导的情况下才会调用此变体, 其中存在{@link PersistenceUnitInfo}
	 * (i.e. {@link LocalContainerEntityManagerFactoryBean}).
	 * 如果通过{@link javax.persistence.Persistence}进行简单的Java SE样式引导 (i.e. {@link LocalEntityManagerFactoryBean}),
	 * 将直接调用无参数{@link #getJpaPropertyMap()}变体.
	 * 
	 * @param pui 当前持久性单元的PersistenceUnitInfo
	 * 
	 * @return JPA属性的Map, 由标准JPA引导程序设施接受; 或空 Map, 如果没有要暴露的属性
	 */
	Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui);

	/**
	 * 返回特定于供应商的JPA属性的Map, 通常基于此JpaVendorAdapter实例中的设置.
	 * <p>请注意, 可能在EntityManagerFactory bean上定义了更多JPA属性, 这可能会覆盖此处指定的各个JPA属性值.
	 * 
	 * @return JPA属性的Map, 由标准JPA引导程序设施接受; 或空 Map, 如果没有要暴露的属性
	 */
	Map<String, ?> getJpaPropertyMap();

	/**
	 * 返回此提供者的特定于供应商的JpaDialect实现, 或{@code null}.
	 */
	JpaDialect getJpaDialect();

	/**
	 * 返回EntityManagerFactory代理应实现的特定于供应商的EntityManagerFactory接口.
	 * <p>如果提供者不提供任何EntityManagerFactory扩展,
	 * 则适配器应该只返回标准{@link javax.persistence.EntityManagerFactory}类.
	 */
	Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface();

	/**
	 * 返回此提供者的EntityManagers将实现的特定于供应商的EntityManager接口.
	 * <p>如果提供者不提供任何EntityManager扩展, 那么适配器应该只返回标准{@link javax.persistence.EntityManager}类.
	 */
	Class<? extends EntityManager> getEntityManagerInterface();

	/**
	 * 在活动使用之前对本机EntityManagerFactory进行后处理的可选回调.
	 * <p>这可用于触发特定于供应商的初始化过程.
	 * 虽然预计不会将其用于大多数提供者, 但它在此处作为一般扩展挂钩包含在内.
	 */
	void postProcessEntityManagerFactory(EntityManagerFactory emf);

}
