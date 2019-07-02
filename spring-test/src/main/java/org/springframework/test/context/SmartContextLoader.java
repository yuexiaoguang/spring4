package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * 用于为Spring TestContext Framework管理的集成测试加载{@link ApplicationContext 应用程序上下文}的策略接口.
 *
 * <p>{@code SmartContextLoader} SPI取代了Spring 2.5中引入的{@link ContextLoader} SPI:
 * {@code SmartContextLoader}可以选择处理资源位置或带注解的类.
 * 此外, {@code SmartContextLoader}可以在其加载的上下文中设置活动Bean定义配置文件
 * (see {@link MergedContextConfiguration#getActiveProfiles()} and {@link #loadContext(MergedContextConfiguration)}).
 *
 * <p>有关<em>带注解的类</em>的定义, 请参阅{@link ContextConfiguration @ContextConfiguration}的Javadoc.
 *
 * <p>{@code SmartContextLoader}的客户端应在调用
 * {@link #loadContext(MergedContextConfiguration) loadContext()}之前调用
 * {@link #processContextConfiguration(ContextConfigurationAttributes) processContextConfiguration()}.
 * 这为{@code SmartContextLoader}提供了修改资源位置或检测默认资源位置或默认配置类的自定义支持的机会.
 * 应该为根测试类的层次结构中的所有类合并
 * {@link #processContextConfiguration(ContextConfigurationAttributes) processContextConfiguration()}的结果,
 * 然后提供给
 * {@link #loadContext(MergedContextConfiguration) loadContext()}.
 *
 * <p>即使{@code SmartContextLoader}扩展 {@code ContextLoader}, 客户端也应该支持{@code SmartContextLoader}特定方法,
 * 而不是{@code ContextLoader}中定义的方法, 特别是因为{@code SmartContextLoader}可能选择不支持{@code ContextLoader} SPI中定义的方法.
 *
 * <p>具体实现必须提供{@code public} 无参构造函数.
 *
 * <p>Spring提供了以下开箱即用的实现:
 * <ul>
 * <li>{@link org.springframework.test.context.support.DelegatingSmartContextLoader DelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericPropertiesContextLoader GenericPropertiesContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.WebDelegatingSmartContextLoader WebDelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader}</li>
 * </ul>
 */
public interface SmartContextLoader extends ContextLoader {

	/**
	 * 处理给定测试类的{@link ContextConfigurationAttributes}.
	 * <p>具体实现可以选择<em>修改</em>提供的{@link ContextConfigurationAttributes}中的
	 * {@code locations}或 {@code classes}, <em>生成</em>默认配置位置,
	 * 或<em>检测</em>默认配置类, 如果提供的值为{@code null}或为空.
	 * <p><b>Note</b>: 与标准{@code ContextLoader}相比, {@code SmartContextLoader} <b>必须</b> <em>抢先</em>
	 * 在设置提供的{@link ContextConfigurationAttributes}中相应的{@code locations} 或 {@code classes}之前验证生成或检测到的默认值是否确实存在.
	 * 因此, 将{@code locations}或{@code classes}属性设置为空, 表示此{@code SmartContextLoader}无法生成或检测到默认值.
	 * 
	 * @param configAttributes 要处理的上下文配置属性
	 */
	void processContextConfiguration(ContextConfigurationAttributes configAttributes);

	/**
	 * 根据提供的{@link MergedContextConfiguration 合并上下文配置} 加载新的{@link ApplicationContext context},
	 * 配置上下文, 最后以完全<em>刷新</em>状态返回上下文.
	 * <p>具体实现应该使用{@code SmartContextLoader}加载的{@link ApplicationContext 应用上下文}的bean工厂注册注解配置处理器.
	 * 因此，Bean将自动成为使用
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
	 * {@link javax.annotation.Resource @Resource}, 和{@link javax.inject.Inject @Inject}
	 * 进行基于注解的依赖注入的候选者.
	 * 此外, 具体实现应该在上下文的{@link org.springframework.core.env.Environment Environment}中设置活动bean定义配置文件.
	 * <p>由{@code SmartContextLoader}加载的任何{@code ApplicationContext}<strong>必须</strong>为自己注册JVM关闭挂钩.
	 * 除非上下文提前关闭, 否则所有上下文实例将在JVM关闭时自动关闭.
	 * 这允许释放上下文中bean所持有的外部资源 (e.g., 临时文件).
	 * 
	 * @param mergedConfig 用于加载应用程序上下文的合并上下文配置
	 * 
	 * @return 新的应用程序上下文
	 * @throws Exception 如果上下文加载失败
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception;

}
