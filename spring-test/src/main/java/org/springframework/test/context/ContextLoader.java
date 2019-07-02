package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * 用于为Spring TestContext Framework管理的集成测试加载{@link ApplicationContext 应用程序上下文}的策略接口.
 *
 * <p><b>Note</b>: 从Spring 3.1开始, 实现{@link SmartContextLoader}而不是此接口,
 * 以便为带注解的类, 活动bean定义配置文件和应用程序上下文初始化器提供支持.
 *
 * <p>ContextLoader的客户端应在调用{@link #loadContext(String...) loadContext()}之前
 * 调用{@link #processLocations(Class, String...) processLocations()},
 * 以防ContextLoader为修改或生成位置提供自定义支持.
 * 然后应将{@link #processLocations(Class, String...) processLocations()}的结果提供给
 * {@link #loadContext(String...) loadContext()}.
 *
 * <p>具体实现必须提供{@code public}无参构造函数.
 *
 * <p>Spring提供了以下开箱即用的实现:
 * <ul>
 * <li>{@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericPropertiesContextLoader GenericPropertiesContextLoader}</li>
 * </ul>
 */
public interface ContextLoader {

	/**
	 * 处理指定类的应用程序上下文资源位置.
	 * <p>具体实现可以选择修改所提供的位置, 生成新位置, 或者简单地返回所提供的位置.
	 * 
	 * @param clazz 与位置关联的类: 用于确定如何处理提供的位置
	 * @param locations 用于加载应用程序上下文的未修改位置 (可以是{@code null}或为空)
	 * 
	 * @return 一组应用程序上下文资源位置
	 */
	String[] processLocations(Class<?> clazz, String... locations);

	/**
	 * 根据提供的{@code locations}加载新的{@link ApplicationContext context},
	 * 配置上下文, 最后以完全<em>刷新</em>状态返回上下文.
	 * <p>默认情况下, 配置位置通常被视为类路径资源.
	 * <p>具体实现应该使用此ContextLoader加载的{@link ApplicationContext 应用程序上下文}的 bean工厂注册注解配置处理器.
	 * 因此, Bean将自动成为使用
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
	 * {@link javax.annotation.Resource @Resource}, 和{@link javax.inject.Inject @Inject}进行基于注解的依赖注入的候选者.
	 * <p>由<strong>必须</strong>为自己注册JVM关闭挂钩的ContextLoader 加载的任何ApplicationContext.
	 * 除非上下文提前关闭, 否则所有上下文实例将在JVM关闭时自动关闭.
	 * 这允许在上下文中释放由bean保持的外部资源, e.g. 临时文件.
	 * 
	 * @param locations 用于加载应用程序上下文的资源位置
	 * 
	 * @return 新的应用程序上下文
	 * @throws Exception 如果上下文加载失败
	 */
	ApplicationContext loadContext(String... locations) throws Exception;

}
