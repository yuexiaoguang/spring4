package org.springframework.orm.jdo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.jdo.JDOException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean}, 创建一个JDO {@link javax.jdo.PersistenceManagerFactory}.
 * 这是在Spring应用程序上下文中设置共享JDO PersistenceManagerFactory的常用方法;
 * 然后可以通过依赖注入将PersistenceManagerFactory传递给基于JDO的DAO.
 * 请注意, 切换到JNDI查找或bean样式的PersistenceManagerFactory实例只是配置问题!
 *
 * <p><b>NOTE: 从Spring 4.0开始, 这个类需要JDO 3.0或更高版本.</b>
 * 它也会公开JPA {@link javax.persistence.EntityManagerFactory},
 * 只要JDO提供者在下面创建一个{@link javax.jdo.JDOEntityManagerFactory}引用,
 * 这意味着这个类可以用作{@link org.springframework.orm.jpa.LocalEntityManagerFactoryBean}的替代.
 *
 * <p>配置可以从属性文件中读取, 指定为"configLocation", 也可以在本地指定.
 * 在此处指定为"jdoProperties"的属性将覆盖文件中的任何设置.
 * 也可以指定"persistenceManagerFactoryName", 引用"META-INF/jdoconfig.xml"中的PMF定义
 * (see {@link #setPersistenceManagerFactoryName}).
 *
 * <p>该类还实现了{@link org.springframework.dao.support.PersistenceExceptionTranslator}接口,
 * 由Spring的{@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}自动检测,
 * 用于基于AOP的Spring DataAccessExceptions的本机异常转换.
 * 因此, LocalPersistenceManagerFactoryBean的存在自动启用PersistenceExceptionTranslationPostProcessor来转换JDO异常.
 *
 * <p><b>备选: PersistenceManagerFactory提供者bean的配置</b>
 *
 * <p>作为此FactoryBean提供的属性驱动方法的替代方法 (类似于将标准JDOHelper类与使用标准JDO属性填充的Properties对象一起使用),
 * 可以直接设置PersistenceManagerFactory实现类的实例.
 *
 * <p>像DataSource一样, 鼓励PersistenceManagerFactory支持bean风格的配置, 这使得设置为Spring管理的bean非常容易.
 * 实现类成为bean类;
 * 其余属性作为bean属性应用 (从小写字符开始, 与相应的JDO属性相反).
 *
 * <p>例如<a href="http://www.jpox.org">JPOX</a>:
 *
 * <p><pre class="code">
 * &lt;bean id="persistenceManagerFactory" class="org.jpox.PersistenceManagerFactoryImpl" destroy-method="close"&gt;
 *   &lt;property name="connectionFactory" ref="dataSource"/&gt;
 *   &lt;property name="nontransactionalRead" value="true"/&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>请注意, 直接设置PersistenceManagerFactory实现是将外部连接工厂(i.e. a JDBC DataSource) 传递到JDO PersistenceManagerFactory的唯一方法.
 * 使用标准的属性驱动方法, 只能使用内部连接池或JNDI DataSource.
 *
 * <p>{@code close()}方法在JDO中是标准化的; 不要忘记为任何PersistenceManagerFactory实例指定它为"destroy-method".
 * 请注意, 此FactoryBean将自动为其创建的PersistenceManagerFactory调用{@code close()}, 而不进行任何特殊配置.
 */
public class LocalPersistenceManagerFactoryBean implements FactoryBean<PersistenceManagerFactory>,
		BeanClassLoaderAware, InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	protected final Log logger = LogFactory.getLog(getClass());

	private String persistenceManagerFactoryName;

	private Resource configLocation;

	private final Map<String, Object> jdoPropertyMap = new HashMap<String, Object>();

	private ClassLoader beanClassLoader;

	private PersistenceManagerFactory persistenceManagerFactory;

	private JdoDialect jdoDialect;


	/**
	 * 指定所需PersistenceManagerFactory的名称.
	 * <p>如果存在这样的资源, 这可能是类路径中的属性资源, 或者是来自"META-INF/jdoconfig.xml"的具有该名称的PMF定义,
	 * 或者JPA EntityManagerFactory根据"META-INF/persistence.xml" (JPA)中的持久性单元名称强制转换为PersistenceManagerFactory.
	 * <p>默认无: 需要指定'persistenceManagerFactoryName'或'configLocation'或'jdoProperties'.
	 */
	public void setPersistenceManagerFactoryName(String persistenceManagerFactoryName) {
		this.persistenceManagerFactoryName = persistenceManagerFactoryName;
	}

	/**
	 * 设置JDO属性配置文件的位置, 例如作为类路径资源"classpath:kodo.properties".
	 * <p>Note: 当通过此bean在本地指定所有必需的属性时, 可以省略.
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置JDO属性, 例如"javax.jdo.PersistenceManagerFactoryClass".
	 * <p>可用于覆盖JDO属性配置文件中的值, 或在本地指定所有必需的属性.
	 * <p>可以使用String "value" (通过PropertiesEditor解析)或XML bean定义中的"props"元素填充.
	 */
	public void setJdoProperties(Properties jdoProperties) {
		CollectionUtils.mergePropertiesIntoMap(jdoProperties, this.jdoPropertyMap);
	}

	/**
	 * 将JDO属性指定为Map, 以传递到{@code JDOHelper.getPersistenceManagerFactory}.
	 * <p>可以在XML bean定义中使用"map"或"props"元素填充.
	 */
	public void setJdoPropertyMap(Map<String, Object> jdoProperties) {
		if (jdoProperties != null) {
			this.jdoPropertyMap.putAll(jdoProperties);
		}
	}

	/**
	 * 允许将对JDO属性的Map访问权限传递给JDOHelper, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"jdoPropertyMap[myKey]".
	 */
	public Map<String, Object> getJdoPropertyMap() {
		return this.jdoPropertyMap;
	}
	/**
	 * 将JDO方言设置为用于此工厂的PersistenceExceptionTranslator功能.
	 * <p>默认值是基于PersistenceManagerFactory的底层DataSource的DefaultJdoDialect.
	 */
	public void setJdoDialect(JdoDialect jdoDialect) {
		this.jdoDialect = jdoDialect;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 初始化给定位置的PersistenceManagerFactory.
	 * 
	 * @throws IllegalArgumentException 在非法属性值的情况下
	 * @throws IOException 如果无法从给定位置加载属性
	 * @throws JDOException 在JDO初始化错误的情况下
	 */
	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, IOException, JDOException {
		if (this.persistenceManagerFactoryName != null) {
			if (this.configLocation != null || !this.jdoPropertyMap.isEmpty()) {
				throw new IllegalStateException("'configLocation'/'jdoProperties' not supported in " +
						"combination with 'persistenceManagerFactoryName' - specify one or the other, not both");
			}
			if (logger.isInfoEnabled()) {
				logger.info("Building new JDO PersistenceManagerFactory for name '" +
						this.persistenceManagerFactoryName + "'");
			}
			this.persistenceManagerFactory = newPersistenceManagerFactory(this.persistenceManagerFactoryName);
		}

		else {
			Map<String, Object> mergedProps = new HashMap<String, Object>();
			if (this.configLocation != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Loading JDO config from [" + this.configLocation + "]");
				}
				CollectionUtils.mergePropertiesIntoMap(
						PropertiesLoaderUtils.loadProperties(this.configLocation), mergedProps);
			}
			mergedProps.putAll(this.jdoPropertyMap);
			logger.info("Building new JDO PersistenceManagerFactory");
			this.persistenceManagerFactory = newPersistenceManagerFactory(mergedProps);
		}

		// 如果没有明确指定, 则构建默认的JdoDialect.
		if (this.jdoDialect == null) {
			this.jdoDialect = new DefaultJdoDialect(this.persistenceManagerFactory.getConnectionFactory());
		}
	}

	/**
	 * 子类可以重写此操作以执行PersistenceManagerFactory实例的自定义初始化, 并为指定的名称创建它.
	 * <p>默认实现调用JDOHelper的{@code getPersistenceManagerFactory(String)}方法.
	 * 自定义实现可以以特定方式准备实例, 或使用自定义PersistenceManagerFactory实现.
	 * 
	 * @param name 所需PersistenceManagerFactory的名称
	 * 
	 * @return PersistenceManagerFactory实例
	 */
	protected PersistenceManagerFactory newPersistenceManagerFactory(String name) {
		return JDOHelper.getPersistenceManagerFactory(name, this.beanClassLoader);
	}

	/**
	 * 子类可以重写此操作以执行PersistenceManagerFactory实例的自定义初始化,
	 * 通过此LocalPersistenceManagerFactoryBean准备的给定Properties创建它.
	 * <p>默认实现调用JDOHelper的{@code getPersistenceManagerFactory(Map)}方法.
	 * 自定义实现可以以特定方式准备实例, 或使用自定义PersistenceManagerFactory实现.
	 * 
	 * @param props 此LocalPersistenceManagerFactoryBean准备的合并属性
	 * 
	 * @return PersistenceManagerFactory实例
	 */
	protected PersistenceManagerFactory newPersistenceManagerFactory(Map<?, ?> props) {
		return JDOHelper.getPersistenceManagerFactory(props, this.beanClassLoader);
	}


	/**
	 * 返回单例PersistenceManagerFactory.
	 */
	@Override
	public PersistenceManagerFactory getObject() {
		return this.persistenceManagerFactory;
	}

	@Override
	public Class<? extends PersistenceManagerFactory> getObjectType() {
		return (this.persistenceManagerFactory != null ?
			this.persistenceManagerFactory.getClass() : PersistenceManagerFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * PersistenceExceptionTranslator接口的实现, 由Spring的PersistenceExceptionTranslationPostProcessor自动检测.
	 * <p>如果它是JDOException, 则转换异常, 最好使用指定的JdoDialect. 否则返回{@code null}以指示未知异常.
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof JDOException) {
			if (this.jdoDialect != null) {
				return this.jdoDialect.translateException((JDOException) ex);
			}
			else {
				return PersistenceManagerFactoryUtils.convertJdoAccessException((JDOException) ex);
			}
		}
		return null;
	}


	/**
	 * 在bean 工厂关闭时关闭PersistenceManagerFactory.
	 */
	@Override
	public void destroy() {
		logger.info("Closing JDO PersistenceManagerFactory");
		this.persistenceManagerFactory.close();
	}
}
