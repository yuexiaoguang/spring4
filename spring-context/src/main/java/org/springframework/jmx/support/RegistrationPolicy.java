package org.springframework.jmx.support;

/**
 * 表示尝试注册已存在的MBean时的注册行为.
 */
public enum RegistrationPolicy {

	/**
	 * 尝试以已存在的名称注册MBean时, 注册失败.
	 */
	FAIL_ON_EXISTING,

	/**
	 * 尝试在已存在的名称下注册MBean时, 注册应忽略受影响的MBean.
	 */
	IGNORE_EXISTING,

	/**
	 * 在尝试以已存在的名称注册MBean时, 注册应替换受影响的MBean.
	 */
	REPLACE_EXISTING;

	/**
	 * 从{@link MBeanRegistrationSupport}注册行为常量转换为{@link RegistrationPolicy}枚举值.
	 * 
	 * @param registrationBehavior {@link MBeanRegistrationSupport}中现有的已弃用的REGISTRATION_* 常量之一.
	 */
	@SuppressWarnings("deprecation")
	static RegistrationPolicy valueOf(int registrationBehavior) {
		switch (registrationBehavior) {
			case MBeanRegistrationSupport.REGISTRATION_IGNORE_EXISTING:
				return RegistrationPolicy.IGNORE_EXISTING;
			case MBeanRegistrationSupport.REGISTRATION_REPLACE_EXISTING:
				return RegistrationPolicy.REPLACE_EXISTING;
			case MBeanRegistrationSupport.REGISTRATION_FAIL_ON_EXISTING:
				return RegistrationPolicy.FAIL_ON_EXISTING;
		}
		throw new IllegalArgumentException(
				"Unknown MBean registration behavior: " + registrationBehavior);
	}

}
