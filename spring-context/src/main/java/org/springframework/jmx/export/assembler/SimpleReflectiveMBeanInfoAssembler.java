package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler}的简单子类,
 * 总是为方法和属性包含投票赞成, 有效地将所有public方法和属性公开为操作和属性.
 */
public class SimpleReflectiveMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	/**
	 * Always returns {@code true}.
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return true;
	}

	/**
	 * Always returns {@code true}.
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return true;
	}

  /**
   * Always returns {@code true}.
   */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return true;
	}

}
