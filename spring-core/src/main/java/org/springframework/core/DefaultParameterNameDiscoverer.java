package org.springframework.core;

import org.springframework.util.ClassUtils;

/**
 * {@link ParameterNameDiscoverer}策略接口的默认实现,
 * 使用Java 8标准反射机制 (如果可用), 并回退到基于ASM的{@link LocalVariableTableParameterNameDiscoverer}以检查类文件中的调试信息.
 *
 * <p>可以通过{@link #addDiscoverer(ParameterNameDiscoverer)}添加更多发现者.
 */
public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

	private static final boolean standardReflectionAvailable = ClassUtils.isPresent(
			"java.lang.reflect.Executable", DefaultParameterNameDiscoverer.class.getClassLoader());


	public DefaultParameterNameDiscoverer() {
		if (standardReflectionAvailable) {
			addDiscoverer(new StandardReflectionParameterNameDiscoverer());
		}
		addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
	}

}
