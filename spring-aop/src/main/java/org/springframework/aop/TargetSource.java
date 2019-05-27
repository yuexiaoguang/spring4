package org.springframework.aop;

/**
 * {@code TargetSource}用于获取AOP调用的当前“目标”，如果周围没有增强选择结束拦截链本身，将通过反射调用.
 *
 * <p>如果 {@code TargetSource}是"static", 它总会返回相同的目标, 允许在AOP框架中进行优化. 动态目标源可以支持池化，热交换等.
 *
 * <p>应用程序开发人员通常不需要直接使用{@code TargetSources}: 这是一个AOP框架接口.
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * 返回此{@link TargetSource}返回的目标类型.
	 * <p>可以返回 {@code null}, 虽然{@code TargetSource}的某些用法可能只适用于预定的目标类.
	 * 
	 * @return 此{@link TargetSource}返回的目标类型
	 */
	@Override
	Class<?> getTargetClass();

	/**
	 * 是否所有对{@link #getTarget()}的调用都会返回相同的对象?
	 * <p>在这种情况下, 不需要调用{@link #releaseTarget(Object)}, 而且AOP框架可以缓存{@link #getTarget()}的返回值.
	 * 
	 * @return {@code true}如果目标是不可变的
	 */
	boolean isStatic();

	/**
	 * 返回目标实例. 在AOP框架调用AOP方法调用的“目标”之前立即调用.
	 * 
	 * @return 包含连接点的目标对象, 或{@code null}如果没有实际的目标实例
	 * @throws Exception 如果目标对象无法解析
	 */
	Object getTarget() throws Exception;

	/**
	 * 释放从{@link #getTarget()}方法获取的给定目标对象.
	 * 
	 * @param target 调用{@link #getTarget()}获取的对象
	 * 
	 * @throws Exception 如果对象无法释放
	 */
	void releaseTarget(Object target) throws Exception;

}
