package org.springframework.mock.jndi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;

/**
 * JNDI命名上下文构建器的简单实现.
 *
 * <p>主要针对测试环境, 每个测试用例都可以适当地配置JNDI, 以便{@code new InitialContext()}将公开所需的对象.
 * 也可用于独立应用程序, e.g. 用于将JDBC DataSource绑定到一个已知的JNDI位置,
 * 以便能够在J2EE容器之外使用传统的J2EE数据访问代码.
 *
 * <p>DataSource实现有多种选择:
 * <ul>
 * <li>{@code SingleConnectionDataSource} (对所有getConnection调用使用相同的Connection)
 * <li>{@code DriverManagerDataSource} (在每个getConnection调用上创建一个新的Connection)
 * <li>Apache的Commons DBCP提供{@code org.apache.commons.dbcp.BasicDataSource} (一个真正的池)
 * </ul>
 *
 * <p>引导代码中的典型用法:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);
 * builder.activate();</pre>
 *
 * 请注意, 由于JNDI限制, 无法在同一JVM中激活多个构建器.
 * 因此, 要重复配置新构建器, 请使用以下代码获取对已激活的构建器或新激活的构建器的引用:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);</pre>
 *
 * 请注意, <i>不应该</i>从此工厂方法调用构建器上的{@code activate()}, 因为在任何情况下都会激活一个.
 *
 * <p>只有在设置时才需要此类的实例.
 * 应用程序在激活后不需要保留对它的引用.
 */
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

	/** 此类绑定到JNDI的实例 */
	private static volatile SimpleNamingContextBuilder activated;

	private static boolean initialized = false;

	private static final Object initializationLock = new Object();


	/**
	 * 检查SimpleNamingContextBuilder是否处于活动状态.
	 * 
	 * @return 当前的SimpleNamingContextBuilder实例, 或{@code null}
	 */
	public static SimpleNamingContextBuilder getCurrentContextBuilder() {
		return activated;
	}

	/**
	 * 如果没有SimpleNamingContextBuilder已经配置JNDI, 创建并激活一个.
	 * 否则, 使用现有的激活的 SimpleNamingContextBuilder, 清除它并返回它.
	 * <p>这主要适用于希望重复从头开始重新初始化JNDI绑定的测试套件.
	 * 
	 * @return 可用于控制JNDI绑定的空的SimpleNamingContextBuilder
	 */
	public static SimpleNamingContextBuilder emptyActivatedContextBuilder() throws NamingException {
		if (activated != null) {
			// 清除已激活的上下文构建器.
			activated.clear();
		}
		else {
			// 创建并激活新的上下文构建器.
			SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
			// activate() 调用将导致对激活变量的赋值.
			builder.activate();
		}
		return activated;
	}


	private final Log logger = LogFactory.getLog(getClass());

	private final Hashtable<String,Object> boundObjects = new Hashtable<String,Object>();


	/**
	 * 通过向JNDI NamingManager注册它来注册上下文构建器.
	 * 请注意, 完成此操作后, {@code new InitialContext()}将始终从此工厂返回上下文.
	 * 使用{@code emptyActivatedContextBuilder()}静态方法获取空上下文 (例如, 在测试方法中).
	 * 
	 * @throws IllegalStateException 如果已经在JNDI NamingManager中注册了命名上下文构建器
	 */
	public void activate() throws IllegalStateException, NamingException {
		logger.info("Activating simple JNDI environment");
		synchronized (initializationLock) {
			if (!initialized) {
				if (NamingManager.hasInitialContextFactoryBuilder()) {
					throw new IllegalStateException(
							"Cannot activate SimpleNamingContextBuilder: there is already a JNDI provider registered. " +
							"Note that JNDI is a JVM-wide service, shared at the JVM system class loader level, " +
							"with no reset option. As a consequence, a JNDI provider must only be registered once per JVM.");
				}
				NamingManager.setInitialContextFactoryBuilder(this);
				initialized = true;
			}
		}
		activated = this;
	}

	/**
	 * 暂时停用此上下文构建器.
	 * 它将保持在JNDI NamingManager中注册, 但将委托给标准JNDI InitialContextFactory (如果已配置), 而不是公开自己的绑定对象.
	 * <p>再次调用{@code activate()}以再次公开此上下文构建器自己的绑定对象.
	 * 这种激活/停用序列可以应用任意次数 (e.g. 在同一VM中运行的较大集成测试套件中).
	 */
	public void deactivate() {
		logger.info("Deactivating simple JNDI environment");
		activated = null;
	}

	/**
	 * 清除此上下文构建器中的所有绑定, 同时保持其活动状态.
	 */
	public void clear() {
		this.boundObjects.clear();
	}

	/**
	 * 对于此上下文构建器将生成的所有命名上下文, 将给定对象绑定在给定名称下.
	 * 
	 * @param name 对象的JNDI名称 (e.g. "java:comp/env/jdbc/myds")
	 * @param obj 要绑定的对象 (e.g. DataSource实现)
	 */
	public void bind(String name, Object obj) {
		if (logger.isInfoEnabled()) {
			logger.info("Static JNDI binding: [" + name + "] = [" + obj + "]");
		}
		this.boundObjects.put(name, obj);
	}


	/**
	 * 简单的InitialContextFactoryBuilder实现, 创建一个新的SimpleNamingContext实例.
	 */
	@Override
	public InitialContextFactory createInitialContextFactory(Hashtable<?,?> environment) {
		if (activated == null && environment != null) {
			Object icf = environment.get(Context.INITIAL_CONTEXT_FACTORY);
			if (icf != null) {
				Class<?> icfClass;
				if (icf instanceof Class) {
					icfClass = (Class<?>) icf;
				}
				else if (icf instanceof String) {
					icfClass = ClassUtils.resolveClassName((String) icf, getClass().getClassLoader());
				}
				else {
					throw new IllegalArgumentException("Invalid value type for environment key [" +
							Context.INITIAL_CONTEXT_FACTORY + "]: " + icf.getClass().getName());
				}
				if (!InitialContextFactory.class.isAssignableFrom(icfClass)) {
					throw new IllegalArgumentException(
							"Specified class does not implement [" + InitialContextFactory.class.getName() + "]: " + icf);
				}
				try {
					return (InitialContextFactory) icfClass.newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Cannot instantiate specified InitialContextFactory: " + icf, ex);
				}
			}
		}

		// Default case...
		return new InitialContextFactory() {
			@Override
			@SuppressWarnings("unchecked")
			public Context getInitialContext(Hashtable<?,?> environment) {
				return new SimpleNamingContext("", boundObjects, (Hashtable<String, Object>) environment);
			}
		};
	}

}
