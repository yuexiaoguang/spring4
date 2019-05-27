package org.springframework.jmx.support;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * 提供用于使用{@link javax.management.MBeanServer}注册MBean的支持.
 * 在给定的{@link ObjectName}中遇到现有的MBean时的行为是完全可配置的, 允许灵活的注册设置.
 *
 * <p>跟踪所有已注册的MBean, 并可通过调用 #{@link #unregisterBeans()}方法注销.
 *
 * <p>通过分别重写{@link #onRegister(ObjectName)}和{@link #onUnregister(ObjectName)}方法时, 子类可以在注册或注销MBean时接收通知.
 *
 * <p>默认情况下, 如果尝试使用已使用的{@link javax.management.ObjectName}注册MBean, 则注册过程将失败.
 *
 * <p>通过将{@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy}属性
 * 设置为{@link RegistrationPolicy#IGNORE_EXISTING}, 注册过程将忽略现有MBean, 使其注册.
 * 这在多个应用程序想要在共享的{@link MBeanServer}中共享公共MBean的设置中很有用.
 *
 * <p>如果需要, 将{@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy}属性
 * 设置为{@link RegistrationPolicy#REPLACE_EXISTING} 将导致在注册期间替换现有MBean.
 * 这在无法保证{@link MBeanServer}状态的情况下非常有用.
 */
public class MBeanRegistrationSupport {

	/**
	 * 常量, 指示在尝试以已存在的名称注册MBean时注册失败.
	 * <p>这是默认的注册行为.
	 * @deprecated since Spring 3.2, in favor of {@link RegistrationPolicy#FAIL_ON_EXISTING}
	 */
	@Deprecated
	public static final int REGISTRATION_FAIL_ON_EXISTING = 0;

	/**
	 * 常量, 指示在尝试以已存在的名称注册MBean时, 应忽略受影响的MBean.
	 * @deprecated since Spring 3.2, in favor of {@link RegistrationPolicy#IGNORE_EXISTING}
	 */
	@Deprecated
	public static final int REGISTRATION_IGNORE_EXISTING = 1;

	/**
	 * 常量, 指示在尝试以已存在的名称注册MBean时, 注册应替换受影响的MBean.
	 * @deprecated since Spring 3.2, in favor of {@link RegistrationPolicy#REPLACE_EXISTING}
	 */
	@Deprecated
	public static final int REGISTRATION_REPLACE_EXISTING = 2;


	/**
	 * 这个类的常量.
	 */
	private static final Constants constants = new Constants(MBeanRegistrationSupport.class);

	/**
	 * 此类的{@code Log}实例.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 用于注册bean的{@code MBeanServer}实例.
	 */
	protected MBeanServer server;

	/**
	 * 此导出器已注册的Bean.
	 */
	private final Set<ObjectName> registeredBeans = new LinkedHashSet<ObjectName>();

	/**
	 * 注册MBean并发现它已存在时, 使用的策略.
	 * 默认情况下会引发异常.
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;


	/**
	 * 指定应注册所有bean的{@code MBeanServer}实例.
	 * {@code MBeanExporter}将尝试查找现有的{@code MBeanServer}.
	 */
	public void setServer(MBeanServer server) {
		this.server = server;
	}

	/**
	 * 返回将注册的bean的{@code MBeanServer}.
	 */
	public final MBeanServer getServer() {
		return this.server;
	}

	/**
	 * 通过相应常量的名称设置注册行为,
	 * e.g. "REGISTRATION_IGNORE_EXISTING".
	 * @deprecated since Spring 3.2, in favor of {@link #setRegistrationPolicy(RegistrationPolicy)}
	 */
	@Deprecated
	public void setRegistrationBehaviorName(String registrationBehavior) {
		setRegistrationBehavior(constants.asNumber(registrationBehavior).intValue());
	}

	/**
	 * 尝试在已存在的{@link javax.management.ObjectName}下注册MBean时, 应采取的操作.
	 * <p>Default is REGISTRATION_FAIL_ON_EXISTING.
	 * @deprecated since Spring 3.2, in favor of {@link #setRegistrationPolicy(RegistrationPolicy)}
	 */
	@Deprecated
	public void setRegistrationBehavior(int registrationBehavior) {
		setRegistrationPolicy(RegistrationPolicy.valueOf(registrationBehavior));
	}

	/**
	 * 尝试在已存在的{@link javax.management.ObjectName}下注册MBean时使用的策略.
	 * 
	 * @param registrationPolicy 要使用的策略
	 */
	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		Assert.notNull(registrationPolicy, "RegistrationPolicy must not be null");
		this.registrationPolicy = registrationPolicy;
	}


	/**
	 * 实际上将MBean注册到服务器.
	 * 可以使用{@link #setRegistrationBehavior(int)}和{@link #setRegistrationBehaviorName(String)}方法配置遇到现有MBean时的行为.
	 * 
	 * @param mbean MBean实例
	 * @param objectName MBean的建议的ObjectName
	 * 
	 * @throws JMException 如果注册失败
	 */
	protected void doRegister(Object mbean, ObjectName objectName) throws JMException {
		ObjectName actualObjectName;

		synchronized (this.registeredBeans) {
			ObjectInstance registeredBean = null;
			try {
				registeredBean = this.server.registerMBean(mbean, objectName);
			}
			catch (InstanceAlreadyExistsException ex) {
				if (this.registrationPolicy == RegistrationPolicy.IGNORE_EXISTING) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring existing MBean at [" + objectName + "]");
					}
				}
				else if (this.registrationPolicy == RegistrationPolicy.REPLACE_EXISTING) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Replacing existing MBean at [" + objectName + "]");
						}
						this.server.unregisterMBean(objectName);
						registeredBean = this.server.registerMBean(mbean, objectName);
					}
					catch (InstanceNotFoundException ex2) {
						if (logger.isErrorEnabled()) {
							logger.error("Unable to replace existing MBean at [" + objectName + "]", ex2);
						}
						throw ex;
					}
				}
				else {
					throw ex;
				}
			}

			// 跟踪注册并通知监听器.
			actualObjectName = (registeredBean != null ? registeredBean.getObjectName() : null);
			if (actualObjectName == null) {
				actualObjectName = objectName;
			}
			this.registeredBeans.add(actualObjectName);
		}

		onRegister(actualObjectName, mbean);
	}

	/**
	 * 注销已由此类实例注册的所有Bean.
	 */
	protected void unregisterBeans() {
		Set<ObjectName> snapshot;
		synchronized (this.registeredBeans) {
			snapshot = new LinkedHashSet<ObjectName>(this.registeredBeans);
		}
		if (!snapshot.isEmpty()) {
			logger.info("Unregistering JMX-exposed beans");
			for (ObjectName objectName : snapshot) {
				doUnregister(objectName);
			}
		}
	}

	/**
	 * 实际从服务器注销指定的MBean.
	 * 
	 * @param objectName MBean的建议的ObjectName
	 */
	protected void doUnregister(ObjectName objectName) {
		boolean actuallyUnregistered = false;

		synchronized (this.registeredBeans) {
			if (this.registeredBeans.remove(objectName)) {
				try {
					// MBean可能已经由外部进程注销
					if (this.server.isRegistered(objectName)) {
						this.server.unregisterMBean(objectName);
						actuallyUnregistered = true;
					}
					else {
						if (logger.isWarnEnabled()) {
							logger.warn("Could not unregister MBean [" + objectName + "] as said MBean " +
									"is not registered (perhaps already unregistered by an external process)");
						}
					}
				}
				catch (JMException ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Could not unregister MBean [" + objectName + "]", ex);
					}
				}
			}
		}

		if (actuallyUnregistered) {
			onUnregister(objectName);
		}
	}

	/**
	 * 返回所有已注册bean的{@link ObjectName ObjectNames}.
	 */
	protected final ObjectName[] getRegisteredObjectNames() {
		synchronized (this.registeredBeans) {
			return this.registeredBeans.toArray(new ObjectName[this.registeredBeans.size()]);
		}
	}


	/**
	 * 在给定的{@link ObjectName}下注册MBean时调用.
	 * 允许子类在注册MBean时执行其他处理.
	 * <p>默认实现委托给{@link #onRegister(ObjectName)}.
	 * 
	 * @param objectName 注册MBean使用的实际{@link ObjectName}
	 * @param mbean 已注册的MBean实例
	 */
	protected void onRegister(ObjectName objectName, Object mbean) {
		onRegister(objectName);
	}

	/**
	 * 在给定的{@link ObjectName}下注册MBean时调用.
	 * 允许子类在注册MBean时执行其他处理.
	 * <p>默认实现为空. 可以在子类中重写.
	 * 
	 * @param objectName 注册MBean使用的实际{@link ObjectName}
	 */
	protected void onRegister(ObjectName objectName) {
	}

	/**
	 * 在给定的{@link ObjectName}下注销MBean时调用.
	 * 允许子类在注销MBean时执行其他处理.
	 * <p>默认实现为空. 可以在子类中重写.
	 * 
	 * @param objectName 注销MBean使用的实际{@link ObjectName}
	 */
	protected void onUnregister(ObjectName objectName) {
	}

}
