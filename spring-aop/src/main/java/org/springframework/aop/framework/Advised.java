package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * 由保存AOP代理工厂配置的类实现的接口. 配置包括Interceptor, 其它增强, Advisor, 和代理接口.
 *
 * <p>从Spring获得的任何AOP代理都可以转换为此接口，以允许操作其AOP增强.
 */
public interface Advised extends TargetClassAware {

	/**
	 * 返回是否冻结了Advised配置, 在这种情况下, 不能更改任何增强.
	 */
	boolean isFrozen();

	/**
	 * 代理完整的目标类, 而不是指定的接口?
	 */
	boolean isProxyTargetClass();

	/**
	 * 返回AOP代理代理的接口.
	 * <p>不包括目标类，也可能代理.
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * 确定给定接口是否被代理.
	 * 
	 * @param intf 要检查的接口
	 */
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * 更改此{@code Advised}对象使用的{@code TargetSource}.
	 * <p>仅在配置不是{@linkplain #isFrozen frozen}时才有效.
	 * 
	 * @param targetSource 要使用的新的TargetSource
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * 返回此{@code Advised}对象使用的{@code TargetSource}.
	 */
	TargetSource getTargetSource();

	/**
	 * 设置代理是否应由AOP框架作为{@link ThreadLocal}公开，以便通过{@link AopContext}类进行检索.
	 * <p>如果增强的对象需要在应用了增强的情况下调用自身的方法，则可能需要公开代理.
	 * 否则, 如果增强的对象在{@code this}上调用方法, 不会应用增强.
	 * <p>默认是 {@code false}, 为了获得最佳性能.
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * 返回工厂是否应将代理公开为{@link ThreadLocal}.
	 * <p>如果增强的对象需要在应用了增强的情况下调用自身的方法，则可能需要公开代理.
	 * 否则, 如果增强的对象在{@code this}上调用方法, 不会应用增强.
	 * <p>获取代理, 类似于EJB调用{@code getEJBObject()}.
	 */
	boolean isExposeProxy();

	/**
	 * 设置是否对此代理配置进行预过滤，以使其仅包含适用的切面 (匹配此代理的目标类).
	 * <p>默认是 "false". 设置为 "true", 如果切面已经预过滤, 意味着在构建代理调用的实际切面链时, 可以跳过ClassFilter检查.
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * 是否对此代理配置进行预过滤，以使其仅包含适用的切面 (匹配此代理的目标类).
	 */
	boolean isPreFiltered();

	/**
	 * 返回应用于此代理的切面.
	 * 
	 * @return 应用于此代理的一组Advisor (never {@code null})
	 */
	Advisor[] getAdvisors();

	/**
	 * 添加一个切面到切面链的结尾.
	 * <p>Advisor可能是一个{@link org.springframework.aop.IntroductionAdvisor},
	 * 接下来从相关工厂获得代理时, 其中的新接口将可用.
	 * 
	 * @param advisor 要添加到链结尾的切面
	 * 
	 * @throws AopConfigException 无效的增强
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * 添加一个Advisor到链中的指定位置.
	 * 
	 * @param advisor 要添加的增强
	 * @param pos 链中的位置 (0 是头). 必须有效.
	 * 
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * 删除指定的切面.
	 * 
	 * @param advisor 要删除的切面
	 * 
	 * @return {@code true} 如果切面被删除; {@code false} 如果切面未找到无法删除
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * 删除给定索引的切面.
	 * 
	 * @param index 要删除的切面的索引
	 * 
	 * @throws AopConfigException 如果index无效
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * 返回给定切面的索引(从 0), 或 -1 如果没有切面应用于这个代理.
	 * <p>此方法的返回值可用于索引到切面数组.
	 * 
	 * @param advisor 要搜索的切面
	 * 
	 * @return 这个切面从 0 开始的索引, 或 -1 如果没有切面
	 */
	int indexOf(Advisor advisor);

	/**
	 * 替换给定的切面.
	 * <p><b>Note:</b> 如果切面是一个{@link org.springframework.aop.IntroductionAdvisor}, 
	 * 并且替换不是或实现了不同的接口, 需要重新获取代理, 否则将不支持旧接口, 并且不会实现新接口.
	 * 
	 * @param a 要替换的切面
	 * @param b 新的切面
	 * 
	 * @return 是否被替换. 如果在切面列表中未找到切面, 此方法返回{@code false}并且不执行任何操作.
	 * @throws AopConfigException 无效的增强
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * 将给定的AOP增强添加到增强（拦截器）链的尾部.
	 * <p>使用切点将这个封装进 DefaultPointcutAdvisor, 该切点始终适用，并以此包装形式从{@code getAdvisors()}方法返回.
	 * <p>请注意，给定的增强将适用于代理上的所有调用, 甚至到{@code toString()} 方法!
	 * 使用适当的增强实现或指定适当的切点以应用于更窄的方法集.
	 * 
	 * @param advice 要添加到链的尾部的增强
	 * 
	 * @throws AopConfigException 无效的增强
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * 将给定的AOP增强添加到增强链的指定位置.
	 * <p>使用切点将这个封装进{@link org.springframework.aop.support.DefaultPointcutAdvisor},
	 * 该切点始终适用，并以此包装形式从{@code getAdvisors()}方法返回.
	 * <p>请注意，给定的增强将适用于代理上的所有调用, 甚至到{@code toString()} 方法!
	 * 使用适当的增强实现或指定适当的切点以应用于更窄的方法集.
	 * 
	 * @param pos 从0 (head)开始的索引
	 * @param advice 要添加的增强
	 * 
	 * @throws AopConfigException 无效的增强
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * 删除包含给定增强的 Advisor.
	 * 
	 * @param advice 要删除的增强
	 * 
	 * @return {@code true}找到并删除了; {@code false}未找到该增强
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * 返回指定的AOP增强的索引(from 0), 或 -1 如果该代理没有指定的增强.
	 * <p>此方法的返回值可用于索引到切面数组.
	 * 
	 * @param advice 要搜索的AOP Alliance增强
	 * 
	 * @return 该增强从 0 开始的索引, 或 -1 如果没有指定的增强
	 */
	int indexOf(Advice advice);

	/**
	 * 因为{@code toString()}通常会被委托给目标, 这将返回AOP代理的等效项.
	 * 
	 * @return 代理配置的字符串描述
	 */
	String toProxyConfigString();

}
