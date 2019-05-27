package org.springframework.aop;

/**
 * 过滤器, 限制切点或引入与给定目标类集的匹配.
 *
 * <p>可以作为{@link Pointcut}的一部分, 或{@link IntroductionAdvisor}的整个目标.
 */
public interface ClassFilter {

	/**
	 * 切点是否应用于给定的接口或目标类?
	 * 
	 * @param clazz 候选目标类
	 * 
	 * @return 增强是否应该适用于给定的目标类
	 */
	boolean matches(Class<?> clazz);


	/**
	 * ClassFilter的规范实例，匹配所有类.
	 */
	ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
