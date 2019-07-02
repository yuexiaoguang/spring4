package org.springframework.test.context;

/**
 * 用于以编程方式解析在为测试类加载
 * {@link org.springframework.context.ApplicationContext ApplicationContext}时,
 * 应使用哪些<em>活动的Bean定义配置文件</em>的策略接口.
 *
 * <p>可以通过{@code @ActiveProfiles}的{@link ActiveProfiles#resolver resolver}属性注册自定义{@code ActiveProfilesResolver}.
 *
 * <p>具体实现必须提供{@code public}无参构造函数.
 */
public interface ActiveProfilesResolver {

	/**
	 * 解析在为给定的{@linkplain Class 测试类}加载{@code ApplicationContext}时使用的<em>bean定义配置文件</em>.
	 * 
	 * @param testClass 应该为其解析配置文件的测试类; never {@code null}
	 * 
	 * @return 加载{@code ApplicationContext}时要使用的bean定义配置文件列表; never {@code null}
	 */
	String[] resolve(Class<?> testClass);

}
